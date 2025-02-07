(ns com.github.clojure-lsp.intellij.extension.init-db-startup
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.InitDBStartup
   :implements [com.intellij.openapi.startup.StartupActivity
                com.intellij.openapi.project.DumbAware])
  (:require
   [com.github.clojure-lsp.intellij.db :as db])
  (:import
   [com.intellij.openapi.project Project]))

(set! *warn-on-reflection* true)

(defn -runActivity [_this ^Project project]
  (db/init-db-for-project project))
