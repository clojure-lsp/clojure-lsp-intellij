(ns com.github.clojure-lsp.intellij.extension.paredit
  "Paredit structural editing mode: typing and deletion handlers that keep the
   brackets balanced while the mode is on, and structural editing actions."
  (:require
   [com.github.clojure-lsp.intellij.notification :as notification]
   [com.github.clojure-lsp.intellij.paredit :as paredit]
   [com.github.clojure-lsp.intellij.settings :as settings]
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]])
  (:import
   [com.github.clojure_lsp.intellij ClojureFileType]
   [com.intellij.codeInsight.editorActions TypedHandlerDelegate TypedHandlerDelegate$Result]
   [com.intellij.openapi.actionSystem AnActionEvent CommonDataKeys]
   [com.intellij.openapi.command WriteCommandAction]
   [com.intellij.openapi.editor Document Editor ScrollType SelectionModel]
   [com.intellij.openapi.fileEditor FileDocumentManager]
   [com.intellij.openapi.ide CopyPasteManager]
   [com.intellij.openapi.project Project]
   [com.intellij.psi PsiFile]
   [java.awt.datatransfer StringSelection]))

(set! *warn-on-reflection* true)

(defn ^:private clojure-editor? [^Editor editor]
  (when-let [file (.getFile (FileDocumentManager/getInstance) (.getDocument editor))]
    (= ClojureFileType/INSTANCE (.getFileType file))))

(defn ^:private text [^Editor editor]
  (str (.getCharsSequence (.getDocument editor))))

(defn ^:private offset [^Editor editor]
  (.getOffset (.getCaretModel editor)))

(defn ^:private apply-result!
  "Applies a paredit operation result to the editor. Must run inside a write
   action."
  [^Editor editor {:keys [edits caret killed]}]
  (let [^Document document (.getDocument editor)]
    (doseq [[start end ^String replacement] (sort-by first > edits)]
      (.replaceString document (int start) (int end) replacement))
    (.removeSelection (.getSelectionModel editor))
    (.moveToOffset (.getCaretModel editor) (int caret))
    (.scrollToCaret (.getScrollingModel editor) ScrollType/RELATIVE)
    (when killed
      (.setContents (CopyPasteManager/getInstance) (StringSelection. ^String killed)))))

(defn ^:private strict-mode? [^Editor editor]
  (and (settings/paredit-enabled?)
       (= 1 (.getCaretCount (.getCaretModel editor)))
       (clojure-editor? editor)))

;; Typing

(defn ^:private typed-result [^Editor editor c]
  (let [text (text editor)
        offset (offset editor)]
    (case c
      (\( \[ \{) (paredit/insert-pair text offset c)
      (\) \] \}) (paredit/insert-closer text offset)
      \" (paredit/insert-quote text offset)
      \; (paredit/insert-semicolon text offset)
      \\ (paredit/insert-backslash text offset)
      nil)))

(defn ^:private selection-result
  "Typing an opening bracket over a selection wraps it; any char replaces
   the selection only when it holds whole forms."
  [^Editor editor c]
  (let [^SelectionModel selection (.getSelectionModel editor)
        text (text editor)
        start (.getSelectionStart selection)
        end (.getSelectionEnd selection)]
    (or (paredit/replace-selection text start end)
        (when (#{\( \[ \{} c)
          (paredit/wrap text start end c)))))

(defn ^:private handle-typed! [^Editor editor result-fn c]
  (if-let [result (and (strict-mode? editor) (result-fn editor c))]
    (do (apply-result! editor result)
        TypedHandlerDelegate$Result/STOP)
    TypedHandlerDelegate$Result/CONTINUE))

(def-extension PareditTypedHandler []
  TypedHandlerDelegate
  (beforeSelectionRemoved [_ c ^Project _project ^Editor editor ^PsiFile _file]
    (handle-typed! editor selection-result c))
  (beforeCharTyped [_ c ^Project _project ^Editor editor ^PsiFile _file _file-type]
    (handle-typed! editor typed-result c)))

;; Deleting

(defn ^:private handle-delete! [^Editor editor delete-fn]
  (when (strict-mode? editor)
    (let [^SelectionModel selection (.getSelectionModel editor)
          text (text editor)]
      (when-let [result (if (.hasSelection selection)
                          (paredit/delete-selection text (.getSelectionStart selection) (.getSelectionEnd selection))
                          (delete-fn text (offset editor)))]
        (apply-result! editor result)
        true))))

(gen-class
 :name com.github.clojure_lsp.intellij.extension.paredit.PareditBackspaceHandler
 :extends com.github.clojure_lsp.intellij.extension.PareditActionHandler
 :constructors {[com.intellij.openapi.editor.actionSystem.EditorActionHandler]
                [com.intellij.openapi.editor.actionSystem.EditorActionHandler]}
 :prefix "backspace-")

(defn backspace-handle [_ editor]
  (boolean (handle-delete! editor paredit/backspace)))

(gen-class
 :name com.github.clojure_lsp.intellij.extension.paredit.PareditDeleteHandler
 :extends com.github.clojure_lsp.intellij.extension.PareditActionHandler
 :constructors {[com.intellij.openapi.editor.actionSystem.EditorActionHandler]
                [com.intellij.openapi.editor.actionSystem.EditorActionHandler]}
 :prefix "delete-")

(defn delete-handle [_ editor]
  (boolean (handle-delete! editor paredit/delete-forward)))

(defn ^:private line-region
  "Start and end of the caret's line, including its line break."
  [^Editor editor]
  (let [^Document document (.getDocument editor)
        line (.getLineNumber document (offset editor))]
    [(.getLineStartOffset document line)
     (min (.getTextLength document) (inc (.getLineEndOffset document line)))]))

(defn ^:private block-unbalanced-removal!
  "Swallows a cut or line deletion that would break the balance. With no
   selection both actions work on the whole caret line."
  [^Editor editor]
  (when (strict-mode? editor)
    (let [^SelectionModel selection (.getSelectionModel editor)
          [start end] (if (.hasSelection selection)
                        [(.getSelectionStart selection) (.getSelectionEnd selection)]
                        (line-region editor))]
      (not (paredit/region-removable? (text editor) start end)))))

(gen-class
 :name com.github.clojure_lsp.intellij.extension.paredit.PareditCutHandler
 :extends com.github.clojure_lsp.intellij.extension.PareditActionHandler
 :constructors {[com.intellij.openapi.editor.actionSystem.EditorActionHandler]
                [com.intellij.openapi.editor.actionSystem.EditorActionHandler]}
 :prefix "cut-")

(defn cut-handle [_ editor]
  (boolean (block-unbalanced-removal! editor)))

(gen-class
 :name com.github.clojure_lsp.intellij.extension.paredit.PareditDeleteLineHandler
 :extends com.github.clojure_lsp.intellij.extension.PareditActionHandler
 :constructors {[com.intellij.openapi.editor.actionSystem.EditorActionHandler]
                [com.intellij.openapi.editor.actionSystem.EditorActionHandler]}
 :prefix "delete-line-")

(defn delete-line-handle [_ editor]
  (boolean (block-unbalanced-removal! editor)))

;; Actions

(defn ^:private on-caret [operation]
  (fn [^Editor editor]
    (operation (text editor) (offset editor))))

(defn ^:private wrap-with [open]
  (fn [^Editor editor]
    (let [^SelectionModel selection (.getSelectionModel editor)]
      (if (.hasSelection selection)
        (paredit/wrap (text editor) (.getSelectionStart selection) (.getSelectionEnd selection) open)
        (paredit/wrap (text editor) (offset editor) (offset editor) open)))))

(def actions
  "Structural editing actions, available whether or not the strict mode is on.
   Key bindings follow Cursive's default keymap."
  [{:name "wrap-round" :text "Wrap with ()" :description "Wrap the form or selection with () (Paredit)" :operation (wrap-with \() :keyboard-shortcut {:first "ctrl shift 9" :replace-all true}}
   {:name "wrap-square" :text "Wrap with []" :description "Wrap the form or selection with [] (Paredit)" :operation (wrap-with \[) :keyboard-shortcut {:first "ctrl OPEN_BRACKET" :replace-all true}}
   {:name "wrap-curly" :text "Wrap with {}" :description "Wrap the form or selection with {} (Paredit)" :operation (wrap-with \{) :keyboard-shortcut {:first "ctrl shift OPEN_BRACKET" :replace-all true}}
   {:name "splice-sexp" :text "Splice sexpr" :description "Remove the delimiters of the current sexpr (Paredit)" :operation (on-caret paredit/splice) :keyboard-shortcut {:first "alt S" :replace-all true}}
   {:name "splice-sexp-killing-backward" :text "Splice sexpr killing backward" :description "Kill the forms before the caret and splice the current sexpr (Paredit)" :operation (on-caret paredit/splice-killing-backward)}
   {:name "splice-sexp-killing-forward" :text "Splice sexpr killing forward" :description "Kill the forms after the caret and splice the current sexpr (Paredit)" :operation (on-caret paredit/splice-killing-forward)}
   {:name "split-sexp" :text "Split sexpr" :description "Split the current sexpr or string at the caret (Paredit)" :operation (on-caret paredit/split) :keyboard-shortcut {:first "alt shift S" :replace-all true}}
   {:name "join-sexps" :text "Join sexprs" :description "Join the sexprs or strings around the caret (Paredit)" :operation (on-caret paredit/join) :keyboard-shortcut {:first "ctrl shift S" :replace-all true}}
   {:name "kill-line" :text "Kill to end of line" :description "Kill to the end of the line or the current sexpr, keeping balance (Paredit)" :operation (on-caret paredit/kill) :keyboard-shortcut {:first "alt D" :replace-all true}}
   {:name "backward-up" :text "Move backward out of sexpr" :description "Move the caret to the start of the current sexpr (Paredit)" :operation (on-caret paredit/backward-up)}
   {:name "forward-up" :text "Move forward out of sexpr" :description "Move the caret past the end of the current sexpr (Paredit)" :operation (on-caret paredit/forward-up)}
   {:name "forward-down" :text "Move forward into sexpr" :description "Move the caret into the next sexpr (Paredit)" :operation (on-caret paredit/forward-down)}
   {:name "backward-down" :text "Move backward into sexpr" :description "Move the caret into the end of the previous sexpr (Paredit)" :operation (on-caret paredit/backward-down)}])

(defn perform-action! [operation ^AnActionEvent event]
  (when-let [editor ^Editor (.getData event CommonDataKeys/EDITOR)]
    (when (clojure-editor? editor)
      (when-let [result (operation editor)]
        (WriteCommandAction/runWriteCommandAction
         (.getProject event)
         ^Runnable (fn [] (apply-result! editor result)))))))

(defn toggle-strict-mode! [^AnActionEvent event]
  (let [enabled? (not (settings/paredit-enabled?))]
    (settings/set-paredit-enabled! enabled?)
    (notification/show-notification!
     {:project (.getProject event)
      :type :info
      :title "Paredit"
      :message (if enabled?
                 "Structural editing mode is on: brackets stay balanced while typing."
                 "Structural editing mode is off.")})))
