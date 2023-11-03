(ns com.github.clojure-lsp.intellij.server
  (:require
   [clojure-lsp.classpath :as lsp.classpath]
   [clojure-lsp.source-paths :as lsp.source-paths]
   [clojure.core.async :as async]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.notification]
   [com.github.clojure-lsp.intellij.project-lsp :as project]
   [com.github.clojure-lsp.intellij.tasks :as tasks]
   [com.github.clojure-lsp.intellij.workspace-edit]
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [lsp4clj.server :as lsp4clj.server])
  (:import
   [com.github.ericdallo.clj4intellij ClojureClassLoader]
   [com.intellij.openapi.project Project]
   [com.intellij.util EnvironmentUtil]))

(def ^:private client-capabilities
  {:text-document {:hover {:content-format ["markdown"]}}
   :workspace {:workspace-edit {:document-changes true}}})

(defn spawn-server! [^Project project]
  (swap! db/db* assoc :status :connecting)
  (run! #(% :connecting) (:on-status-changed-fns @db/db*))
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
           :client client)
    (logger/info "Starting LSP server and client...")
    (tasks/run-background-task!
     project
     "Clojure LSP startup"
     (fn [indicator]
       (ClojureClassLoader/bind)
       (tasks/set-progress indicator "LSP: Starting server")
       (lsp-client/start-server-and-client! server client {:progress-indicator indicator})
       (tasks/set-progress indicator "LSP: Initializing")
       @(lsp-client/request! client [:initialize
                                     {:root-uri (project/project->root-uri project)
                                      :work-done-token "lsp-startup"
                                      :initialization-options (merge {:dependency-scheme "jar"
                                                                      :hover {:arity-on-same-line? true}
                                                                      ;; For some users like brew users, System/getEnv doesn't match
                                                                      ;; User env, so we inject the env that intellij magically calculates
                                                                      ;; from EnvironemntUtil class and pass to clojure-lsp as default.
                                                                      :project-specs (mapv (fn [project-spec]
                                                                                             (assoc project-spec :env (EnvironmentUtil/getEnvironmentMap)))
                                                                                           (lsp.classpath/default-project-specs lsp.source-paths/default-source-aliases))}
                                                                     (:settings @db/db*))
                                      :capabilities client-capabilities}])
       (lsp-client/notify! client [:initialized {}])
       (swap! db/db* assoc :status :connected)
       (run! #(% :connected) (:on-status-changed-fns @db/db*))
       (logger/info "Initialized LSP server...")))
    true))

(defn shutdown! []
  (when-let [client (lsp-client/connected-client)]
    @(lsp-client/request! client [:shutdown {}])
    (swap! db/db* assoc :status :disconnected
           :client nil
           :server nil
           :diagnostics {})
    (run! #(% :disconnected) (:on-status-changed-fns @db/db*))))
