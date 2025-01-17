(ns com.github.clojure-lsp.intellij.extension.init-db-startup
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.InitDBStartup
   :implements [com.intellij.openapi.startup.StartupActivity
                com.intellij.openapi.project.DumbAware])
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.ericdallo.clj4intellij.logger :as logger])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]
   [com.intellij.openapi.project Project]
   [com.redhat.devtools.lsp4ij LanguageServerManager LanguageServerManager$StartOptions]))

(set! *warn-on-reflection* true)

(defn -runActivity [_this ^Project project]
  (db/init-db-for-project project)
  (db/load-settings-from-state! project (SettingsState/get))
  #_(.start (LanguageServerManager/getInstance (first (db/all-projects))) "clojure-lsp" )
  (logger/info "Loaded settings to memory:" (db/get-in project [:settings])))
