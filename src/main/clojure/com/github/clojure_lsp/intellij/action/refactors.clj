(ns com.github.clojure-lsp.intellij.action.refactors
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.ericdallo.clj4intellij.tasks :as tasks]
   [com.github.ericdallo.clj4intellij.util :as util])
  (:import
   [com.intellij.codeInsight.hint HintManager]
   [com.intellij.openapi.actionSystem AnActionEvent]
   [com.intellij.openapi.actionSystem CommonDataKeys]
   [com.intellij.openapi.editor Editor]))

(set! *warn-on-reflection* true)

(defn execute-refactor-action [command-name ^AnActionEvent event]
  (when-let [editor ^Editor (.getData event CommonDataKeys/EDITOR)]
    (if-let [client (lsp-client/connected-server (.getProject editor))]
      (let [[line character] (util/editor->cursor-position editor)]
        (tasks/run-background-task!
         (.getProject editor)
         "LSP: refactoring"
         (fn [_]
           (lsp-client/request! client [:workspace/executeCommand
                                        {:command command-name
                                         :arguments [(editor/editor->uri editor) line character]}]))))
      (.showErrorHint (HintManager/getInstance) ^Editor editor "LSP not connected" HintManager/RIGHT))))
