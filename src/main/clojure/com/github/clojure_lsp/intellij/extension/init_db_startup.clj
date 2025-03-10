(ns com.github.clojure-lsp.intellij.extension.init-db-startup
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]])
  (:import
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.startup StartupActivity]))

(set! *warn-on-reflection* true)

(def-extension InitDBStartup []
  StartupActivity
  (runActivity [_this ^Project project]
    (db/init-db-for-project project)))
