(ns com.github.clojure-lsp.intellij.server
  (:require
   [clojure.core.async :as async]
   [clojure.java.io :as io]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.logger :as logger]
   [com.github.clojure-lsp.intellij.lsp-client :as lsp-client]
   [com.github.clojure-lsp.intellij.tasks :as tasks]
   [lsp4clj.server :as lsp4clj.server])
  (:import
   [com.github.clojure_lsp.intellij WithLoader]
   [com.intellij.openapi.project Project]))

(def ^:private client-capabilities
  {:initialization-options {:dependency-scheme "jar"
                            :hover {:arity-on-same-line? true}}
   :text-document {:hover {:content-format ["markdown"]}}})

(defn spawn-server! [^Project project]
  (let [log-ch (async/chan (async/sliding-buffer 20))
        input-ch (async/chan 1)
        output-ch (async/chan 1)
        server (lsp4clj.server/chan-server {:input-ch input-ch
                                            :output-ch output-ch
                                            :log-ch log-ch
                                            :trace-ch log-ch
                                            :trace-level (-> @db/db* :settings :trace-level)})
        client (lsp-client/client server)]
    (swap! db/db* assoc
           :server server
           :client client
           :status :disconnected)
    (run! #(% :disconnected) (:on-status-changed-fns @db/db*))
    (logger/info "Starting LSP server and client...")
    (tasks/run-background-task!
     project
     "Clojure LSP startup"
     (fn [indicator]
       (WithLoader/bind)
       (tasks/set-progress indicator "LSP: Starting server")
       (lsp-client/start-server-and-client! server client {:progress-indicator indicator})
       (logger/info "Initializing LSP server...")
       (tasks/set-progress indicator "LSP: Initializing")
       @(lsp-client/request! client [:initialize
                                     {:root-uri (-> (.getBasePath project) io/file .toPath .toUri str)
                                      :work-done-token "lsp-startup"
                                      :capabilities client-capabilities}])
       (lsp-client/notify! client [:initialized {}])
       (swap! db/db* assoc :status :connected)
       (run! #(% :connected) (:on-status-changed-fns @db/db*))
       (logger/info "Initialized LSP server...")))))

(defn shutdown! []
  @(lsp-client/request! (:client @db/db*) [:shutdown {}]))
