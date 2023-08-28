(ns com.github.clojure-lsp.intellij.workspace-edit
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor]))

(defmethod lsp-client/workspace-apply-edit :default [{:keys [label edit]}]
  @(editor/apply-workspace-edit (:project @db/db*) label false edit)
  {:applied true})
