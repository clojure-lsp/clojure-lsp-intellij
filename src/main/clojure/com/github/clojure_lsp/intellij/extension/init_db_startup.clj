(ns com.github.clojure-lsp.intellij.extension.init-db-startup
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.InitDBStartup
   :implements [com.intellij.openapi.startup.StartupActivity
                com.intellij.openapi.project.DumbAware])
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.intellij.openapi.project Project]
   [com.redhat.devtools.lsp4ij LanguageServerWrapper]
   [com.redhat.devtools.lsp4ij.lifecycle LanguageServerLifecycleListener LanguageServerLifecycleManager]))

(set! *warn-on-reflection* true)

(defn -runActivity [_this ^Project project]
  (db/init-db-for-project project)
  (.addLanguageServerLifecycleListener
   (LanguageServerLifecycleManager/getInstance project)
   (proxy+ [] LanguageServerLifecycleListener
     (handleStatusChanged [_ ^LanguageServerWrapper server-wrapper]
       (let [status (keyword (.toString (.getServerStatus server-wrapper)))]
         (db/assoc-in project [:status] status)
         (run! #(% status) (db/get-in project [:on-status-changed-fns]))))
     (handleLSPMessage [_ _ _ _])
     (handleError [_ _ _])
     (dispose [_]))))
