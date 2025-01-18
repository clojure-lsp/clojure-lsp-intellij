(ns com.github.clojure-lsp.intellij.extension.init-db-startup
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.InitDBStartup
   :implements [com.intellij.openapi.startup.StartupActivity
                com.intellij.openapi.project.DumbAware])
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]
   [com.intellij.openapi.project Project]
   [com.redhat.devtools.lsp4ij LanguageServerManager LanguageServerWrapper]
   [com.redhat.devtools.lsp4ij.lifecycle LanguageServerLifecycleListener LanguageServerLifecycleManager]))

(set! *warn-on-reflection* true)

(defn -runActivity [_this ^Project project]
  (db/init-db-for-project project)
  (db/load-settings-from-state! project (SettingsState/get))
  (logger/info "Loaded settings to memory:" (db/get-in project [:settings]))
  (.addLanguageServerLifecycleListener
   (LanguageServerLifecycleManager/getInstance project)
   (proxy+ [] LanguageServerLifecycleListener
     (handleStatusChanged [_ ^LanguageServerWrapper server-wrapper]
       (let [status (keyword (.toString (.getServerStatus server-wrapper)))]
         (db/assoc-in project [:status] status)
         (when (= :started status)
           (db/assoc-in project [:server] (.getLanguageServer (LanguageServerManager/getInstance project)
                                                              "clojure-lsp")))
         (run! #(% status) (db/get-in project [:on-status-changed-fns]))))
     (handleLSPMessage [_ _ _ _])
     (handleError [_ _ _])
     (dispose [_]))))
