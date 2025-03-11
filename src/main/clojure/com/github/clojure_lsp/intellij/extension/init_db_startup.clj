(ns com.github.clojure-lsp.intellij.extension.init-db-startup
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]])
  (:import
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.startup ProjectActivity]
   [kotlinx.coroutines CoroutineScope]))

(set! *warn-on-reflection* true)

(def-extension InitDBStartup []
  ProjectActivity
  (execute [_this ^Project project ^CoroutineScope _]
    (db/init-db-for-project project)))
