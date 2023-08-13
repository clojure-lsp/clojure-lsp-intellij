(ns com.github.clojure-lsp.intellij.extension.startup
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.logger :as logger]
   [com.github.clojure-lsp.intellij.lsp-client :as lsp-client]
   [com.github.clojure-lsp.intellij.server :as server]
   [com.github.clojure-lsp.intellij.tasks :as tasks])
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.Startup
   :implements [com.intellij.openapi.startup.StartupActivity
                com.intellij.openapi.project.DumbAware])
  (:import
   [com.github.clojure_lsp.intellij WithLoader]
   [com.github.clojure_lsp.intellij.extension SettingsState]
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
  (WithLoader/bind)
  (logger/info "Starting clojure-lsp plugin...")
  (start-nrepl-server 6660)
  (swap! db/db* assoc :project project)
  (db/load-settings-from-state! (SettingsState/get))
  (server/spawn-server! project))

(defmethod lsp-client/progress "lsp-startup" [{:keys [progress-indicator]} {{:keys [title percentage]} :value}]
  (tasks/set-progress progress-indicator (str "LSP: " title) percentage))
