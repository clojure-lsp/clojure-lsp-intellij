(ns com.github.clojure-lsp.intellij.extension.startup
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.Startup
   :implements [com.intellij.openapi.startup.StartupActivity
                com.intellij.openapi.project.DumbAware])
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.logger :as logger])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]
   [com.github.ericdallo.clj4intellij ClojureClassLoader]
   [com.intellij.openapi.project Project]))

(set! *warn-on-reflection* true)

(defn ^:private start-nrepl-server [port]
  (try
    ((requiring-resolve 'nrepl.server/start-server)
     :port port)
    (logger/info "Started nrepl server at port %s" port)
    (catch Exception e
      (logger/warn "No debug nrepl found %s" e))))

(defn -runActivity [_this ^Project project]
  (ClojureClassLoader/bind)
  (logger/info "Starting clojure-lsp plugin...")
  (swap! db/db* assoc :project project)
  (start-nrepl-server 6660)
  (db/load-settings-from-state! (SettingsState/get)))
