(ns com.github.clojure-lsp.intellij.extension.startup
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.Startup
   :implements [com.intellij.openapi.startup.StartupActivity
                com.intellij.openapi.project.DumbAware])
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.ericdallo.clj4intellij.logger :as logger])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]
   [com.intellij.openapi.project Project]))

(set! *warn-on-reflection* true)

(defn -runActivity [_this ^Project project]
  (logger/info "Starting clojure-lsp plugin...")
  (swap! db/db* assoc :project project)
  (db/load-settings-from-state! (SettingsState/get)))
