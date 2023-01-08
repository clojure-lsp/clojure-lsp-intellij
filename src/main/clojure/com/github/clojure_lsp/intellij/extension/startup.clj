(ns com.github.clojure-lsp.intellij.extension.startup
  (:require
   [com.github.clojure-lsp.intellij.logger :as logger])
  (:gen-class
   :main false
   :name com.github.clojure_lsp.intellij.extension.Startup
   :extends com.github.clojure_lsp.intellij.WithLoader
   :implements [com.intellij.openapi.startup.StartupActivity
                com.intellij.openapi.project.DumbAware])
  (:import
   (com.intellij.openapi.project Project)))

(set! *warn-on-reflection* true)

(defn ^:private start-nrepl-server [port]
  (try
    ((requiring-resolve 'nrepl.server/start-server)
     :port port)
    (logger/info "Started nrepl server at port %s" port)
    (catch Exception _e)))

(defn -runActivity [_this ^Project _project]
  (logger/info "Starting clojure-lsp plugin...")
  (start-nrepl-server 6660))
