(ns com.github.clojure-lsp.intellij.extension.startup
  (:require
   [clojure.core.async :as async]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.logger :as logger]
   [com.github.clojure-lsp.intellij.lsp-client :as lsp-client]
   [lsp4clj.server :as lsp4clj.server])
  (:gen-class
   :main false
   :name com.github.clojure_lsp.intellij.extension.Startup
   :extends com.github.clojure_lsp.intellij.WithLoader
   :implements [com.intellij.openapi.startup.StartupActivity
                com.intellij.openapi.project.DumbAware])
  (:import
   (com.github.clojure_lsp.intellij WithLoader)
   (com.intellij.openapi.project Project)))

(set! *warn-on-reflection* true)

(defn ^:private start-nrepl-server [port]
  (try
    ((requiring-resolve 'nrepl.server/start-server)
     :port port)
    (logger/info "Started nrepl server at port %s" port)
    (catch Exception e
      (logger/warn "No debug nrepl found %s" e))))

(defn -runActivity [_this ^Project _project]
  (WithLoader/bind)
  (logger/info "Starting clojure-lsp plugin...")
  (start-nrepl-server 6660)
  (let [log-ch (async/chan (async/sliding-buffer 20))
        input-ch (async/chan 1)
        output-ch (async/chan 1)
        server (lsp4clj.server/chan-server {:input-ch input-ch
                                            :output-ch output-ch
                                            :log-ch log-ch
                                            :trace-ch log-ch
                                            :trace-level "verbose"})
        client (lsp-client/client server)]
    (logger/info "Starting LSP server and client...")
    (swap! db/db assoc
           :server server
           :client client)
    (lsp-client/start-server-and-client! server client)
    (logger/info "Sending initialize message...")
    @(lsp-client/request! client [:initialize
                                  {:root-uri "file:///home/greg/dev/clojure-sample/"
                                   :work-done-token "1"
                                   :capabilities {}}])
    (lsp-client/notify! client [:initialized {}])
    (logger/info "Initialized LSP server...")))

(comment
  (-runActivity nil nil))
