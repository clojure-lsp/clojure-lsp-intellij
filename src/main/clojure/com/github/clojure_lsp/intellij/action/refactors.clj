(ns com.github.clojure-lsp.intellij.action.refactors
  (:require
   [camel-snake-kebab.core :as csk]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.tasks :as tasks])
  (:import
   [com.intellij.openapi.actionSystem AnActionEvent]
   [com.intellij.openapi.actionSystem CommonDataKeys]
   [com.intellij.openapi.editor Editor]))

(set! *warn-on-reflection* true)

(defn ^:private action-performed [command _ ^AnActionEvent event]
  (when-let [editor ^Editor (.getData event CommonDataKeys/EDITOR)]
    (when-let [client (lsp-client/connected-client (.getProject editor))]
      (let [[line character] (editor/editor->cursor-position editor)]
        (tasks/run-background-task!
         (.getProject editor)
         "LSP: refactoring"
         (fn [_]
           (lsp-client/request! client [:workspace/executeCommand
                                        {:command command
                                         :arguments [(editor/editor->uri editor) line character]}])))))))

(defmacro ^:private gen-refactor [& {:keys [command]}]
  `(do
     (def ~(symbol (str (str command "-") "actionPerformed")) (partial action-performed ~command))
     (gen-class
      :name ~(str "com.github.clojure_lsp.intellij.action." (csk/->PascalCase command) "Action")
      :prefix ~(str command "-")
      :extends "com.intellij.openapi.project.DumbAwareAction")))

(gen-refactor :command "add-missing-import")
(gen-refactor :command "add-missing-libspec")
(gen-refactor :command "add-require-suggestion")
(gen-refactor :command "cycle-coll")
(gen-refactor :command "cycle-keyword-auto-resolve")
(gen-refactor :command "clean-ns")
(gen-refactor :command "cycle-privacy")
(gen-refactor :command "create-test")
(gen-refactor :command "drag-param-backward")
(gen-refactor :command "drag-param-forward")
(gen-refactor :command "drag-backward")
(gen-refactor :command "drag-forward")
(gen-refactor :command "demote-fn")
(gen-refactor :command "destructure-keys")
(gen-refactor :command "extract-to-def")
(gen-refactor :command "extract-function")
(gen-refactor :command "expand-let")
(gen-refactor :command "create-function")
(gen-refactor :command "get-in-all")
(gen-refactor :command "get-in-less")
(gen-refactor :command "get-in-more")
(gen-refactor :command "get-in-none")
(gen-refactor :command "introduce-let")
(gen-refactor :command "inline-symbol")
(gen-refactor :command "resolve-macro-as")
(gen-refactor :command "move-form")
(gen-refactor :command "move-to-let")
(gen-refactor :command "promote-fn")
(gen-refactor :command "replace-refer-all-with-refer")
(gen-refactor :command "replace-refer-all-with-alias")
(gen-refactor :command "restructure-keys")
(gen-refactor :command "change-coll")
(gen-refactor :command "sort-clauses")
(gen-refactor :command "thread-first-all")
(gen-refactor :command "thread-first")
(gen-refactor :command "thread-last-all")
(gen-refactor :command "thread-last")
(gen-refactor :command "unwind-all")
(gen-refactor :command "unwind-thread")
(gen-refactor :command "forward-slurp")
(gen-refactor :command "forward-barf")
(gen-refactor :command "backward-slurp")
(gen-refactor :command "backward-barf")
(gen-refactor :command "raise-sexp")
(gen-refactor :command "kill-sexp")
