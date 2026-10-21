(ns com.github.clojure-lsp.intellij.extension.paredit
  "Paredit structural editing mode: typing and deletion handlers that keep the
   brackets balanced while the mode is on, and structural editing actions."
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.notification :as notification]
   [com.github.clojure-lsp.intellij.paredit :as paredit]
   [com.github.clojure-lsp.intellij.settings :as settings]
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]])
  (:import
   [com.github.clojure_lsp.intellij ClojureFileType]
   [com.github.clojure_lsp.intellij.extension PareditWriteGuardHandler]
   [com.intellij.codeInsight.editorActions TypedHandlerDelegate TypedHandlerDelegate$Result]
   [com.intellij.codeInsight.hint HintManager]
   [com.intellij.openapi.actionSystem AnActionEvent CommonDataKeys DataContext]
   [com.intellij.openapi.command WriteCommandAction]
   [com.intellij.openapi.editor Caret Document Editor ScrollType SelectionModel]
   [com.intellij.openapi.editor.actionSystem EditorActionHandler]
   [com.intellij.openapi.fileEditor FileDocumentManager]
   [com.intellij.openapi.ide CopyPasteManager]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.util SystemInfo]
   [com.intellij.openapi.wm WindowManager]
   [com.intellij.psi PsiFile]
   [java.awt.datatransfer DataFlavor StringSelection]))

(set! *warn-on-reflection* true)

(def widget-id "ClojureLSPParedit")

(defn ^:private clojure-editor? [^Editor editor]
  (when-let [file (.getFile (FileDocumentManager/getInstance) (.getDocument editor))]
    (= ClojureFileType/INSTANCE (.getFileType file))))

(defn ^:private text [^Editor editor]
  (str (.getCharsSequence (.getDocument editor))))

(defn ^:private offset [^Editor editor]
  (.getOffset (.getCaretModel editor)))

(defn ^:private single-caret? [^Editor editor]
  (= 1 (.getCaretCount (.getCaretModel editor))))

(defn ^:private strict-mode? [^Editor editor]
  (and (settings/paredit-enabled?)
       (clojure-editor? editor)))

(defn ^:private show-hint! [^Editor editor ^String message]
  (.showInformationHint (HintManager/getInstance) editor message))

(defn ^:private apply-edits! [^Document document edits]
  (doseq [[start end ^String replacement] (sort-by first > edits)]
    (.replaceString document (int start) (int end) replacement)))

(defn ^:private apply-result!
  "Applies a paredit operation result at the current caret. Must run inside a
   write action."
  [^Editor editor {:keys [edits caret killed selection]}]
  (apply-edits! (.getDocument editor) edits)
  (.removeSelection (.getSelectionModel editor))
  (.moveToOffset (.getCaretModel editor) (int caret))
  (when-let [[start end] selection]
    (.setSelection (.getSelectionModel editor) (int start) (int end)))
  (.scrollToCaret (.getScrollingModel editor) ScrollType/RELATIVE)
  (when killed
    (.setContents (CopyPasteManager/getInstance) (StringSelection. ^String killed))))

;; Typing

(defn ^:private typed-result [text offset c]
  (case c
    (\( \[ \{) (paredit/insert-pair text offset c)
    (\) \] \}) (paredit/insert-closer text offset)
    \" (paredit/insert-quote text offset)
    \; (paredit/insert-semicolon text offset)
    \\ (paredit/insert-backslash text offset)
    nil))

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

(defn ^:private handle-typed!
  "The platform calls typed handlers once per caret, with that caret current."
  [^Editor editor result]
  (if result
    (do (apply-result! editor result)
        TypedHandlerDelegate$Result/STOP)
    TypedHandlerDelegate$Result/CONTINUE))

(def-extension PareditTypedHandler []
  TypedHandlerDelegate
  (beforeSelectionRemoved [_ c ^Project _project ^Editor editor ^PsiFile _file]
    (handle-typed! editor (and (strict-mode? editor) (selection-result editor c))))
  (beforeCharTyped [_ c ^Project _project ^Editor editor ^PsiFile _file _file-type]
    (handle-typed! editor (and (strict-mode? editor) (typed-result (text editor) (offset editor) c)))))

;; Deleting

(defn ^:private handle-delete!
  "Runs at each caret in turn, with that caret current."
  [^Editor editor delete-fn]
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

(defn backspace-handle [_ editor _caret _data-context]
  (boolean (handle-delete! editor paredit/backspace)))

(gen-class
 :name com.github.clojure_lsp.intellij.extension.paredit.PareditDeleteHandler
 :extends com.github.clojure_lsp.intellij.extension.PareditActionHandler
 :constructors {[com.intellij.openapi.editor.actionSystem.EditorActionHandler]
                [com.intellij.openapi.editor.actionSystem.EditorActionHandler]}
 :prefix "delete-")

(defn delete-handle [_ editor _caret _data-context]
  (boolean (handle-delete! editor paredit/delete-forward)))

;; Guarding other ways of removing or inserting text

(def ^:private unbalanced-message "Paredit: this would leave unbalanced brackets")

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
  (when (and (strict-mode? editor) (single-caret? editor))
    (let [^SelectionModel selection (.getSelectionModel editor)
          [start end] (if (.hasSelection selection)
                        [(.getSelectionStart selection) (.getSelectionEnd selection)]
                        (line-region editor))]
      (when-not (paredit/region-removable? (text editor) start end)
        (show-hint! editor unbalanced-message)
        true))))

(defn ^:private block-unbalanced-paste!
  "Swallows a paste whose text would break the balance where it lands."
  [^Editor editor]
  (when (and (strict-mode? editor) (single-caret? editor))
    (let [pasted (.getContents (CopyPasteManager/getInstance) DataFlavor/stringFlavor)
          ^SelectionModel selection (.getSelectionModel editor)
          [start end] (if (.hasSelection selection)
                        [(.getSelectionStart selection) (.getSelectionEnd selection)]
                        [(offset editor) (offset editor)])]
      (when (and (string? pasted)
                 (not (paredit/replacement-keeps-balance? (text editor) start end pasted)))
        (show-hint! editor unbalanced-message)
        true))))

(defn ^:private revert-unbalancing!
  "Runs the platform handler, then undoes its change if it broke the balance.
   Used for actions whose exact range is up to the platform, like deleting to
   the word start or joining lines."
  [^PareditWriteGuardHandler handler ^Editor editor ^Caret caret ^DataContext data-context]
  (let [^EditorActionHandler original (.getOriginal handler)]
    (if (and (strict-mode? editor) (single-caret? editor))
      (let [before (text editor)
            caret-offset (offset editor)]
        (.execute original editor caret data-context)
        (let [after (text editor)]
          (when-not (paredit/change-keeps-balance? before after)
            (let [[start end replacement] (paredit/restoring-edit after before)]
              (.replaceString (.getDocument editor) (int start) (int end) ^String replacement)
              (.moveToOffset (.getCaretModel editor) (int caret-offset)))
            (show-hint! editor unbalanced-message))))
      (.execute original editor caret data-context))
    true))

(defmacro ^:private def-guard-handler [class-name super-class prefix handle-body]
  `(do
     (gen-class
      :name ~(symbol (str "com.github.clojure_lsp.intellij.extension.paredit." class-name))
      :extends ~(symbol (str "com.github.clojure_lsp.intellij.extension." super-class))
      :constructors {[com.intellij.openapi.editor.actionSystem.EditorActionHandler]
                     [com.intellij.openapi.editor.actionSystem.EditorActionHandler]}
      :prefix ~prefix)
     (defn ~(symbol (str prefix "handle")) [~'this ~'editor ~'caret ~'data-context]
       ~handle-body)))

(def-guard-handler "PareditCutHandler" "PareditGuardHandler" "cut-"
  (boolean (block-unbalanced-removal! editor)))

(def-guard-handler "PareditDeleteLineHandler" "PareditWriteGuardHandler" "delete-line-"
  (boolean (block-unbalanced-removal! editor)))

(def-guard-handler "PareditPasteHandler" "PareditGuardHandler" "paste-"
  (boolean (block-unbalanced-paste! editor)))

(def-guard-handler "PareditRevertingHandler" "PareditWriteGuardHandler" "reverting-"
  (revert-unbalancing! this editor caret data-context))

;; Actions

(defn ^:private on-caret [operation]
  (fn [^Editor editor]
    (operation (text editor) (offset editor))))

(defn ^:private selection-or-caret [^Editor editor]
  (let [^SelectionModel selection (.getSelectionModel editor)]
    (if (.hasSelection selection)
      [(.getSelectionStart selection) (.getSelectionEnd selection)]
      [(offset editor) (offset editor)])))

(defn ^:private on-selection [operation]
  (fn [^Editor editor]
    (let [[start end] (selection-or-caret editor)]
      (operation (text editor) start end))))

(defn ^:private wrap-with [open]
  (on-selection #(paredit/wrap %1 %2 %3 open)))

(defn ^:private shortcut
  "A key binding, with a different one on macOS when Cursive uses one."
  ([keys] (shortcut keys keys))
  ([keys mac-keys]
   {:first (if SystemInfo/isMac mac-keys keys) :replace-all true}))

(def actions
  "Structural editing actions, available whether or not the strict mode is on.
   The first ten keep the ids and key bindings they had when clojure-lsp ran
   them on the server; the new ones follow Cursive's default keymap."
  [{:name "forward-slurp" :text "Slurp forward" :description "Slurp forward (Paredit)" :operation (on-caret paredit/forward-slurp) :keyboard-shortcut (shortcut "alt CLOSE_BRACKET")}
   {:name "forward-barf" :text "Barf forward" :description "Barf forward (Paredit)" :operation (on-caret paredit/forward-barf) :keyboard-shortcut (shortcut "alt OPEN_BRACKET")}
   {:name "backward-slurp" :text "Slurp backward" :description "Slurp backward (Paredit)" :operation (on-caret paredit/backward-slurp) :keyboard-shortcut (shortcut "alt shift CLOSE_BRACKET")}
   {:name "backward-barf" :text "Barf backward" :description "Barf backward (Paredit)" :operation (on-caret paredit/backward-barf) :keyboard-shortcut (shortcut "alt shift OPEN_BRACKET")}
   {:name "raise-sexp" :text "Raise sexpr" :description "Raise current sexpr (Paredit)" :operation (on-caret paredit/raise-sexp) :keyboard-shortcut (shortcut "alt R")}
   {:name "kill-sexp" :text "Kill sexpr" :description "Kill current sexpr (Paredit)" :operation (on-caret paredit/kill-sexp) :keyboard-shortcut (shortcut "alt K")}
   {:name "forward" :text "Move forward" :description "Move cursor forward a sexpr (Paredit)" :operation (on-caret paredit/forward-sexp) :keyboard-shortcut (shortcut "ctrl alt CLOSE_BRACKET")}
   {:name "forward-select" :text "Select forward" :description "Select forward a sexpr (Paredit)" :operation (on-selection paredit/forward-select) :keyboard-shortcut (shortcut "ctrl shift alt CLOSE_BRACKET")}
   {:name "backward" :text "Move backward" :description "Move cursor backward a sexpr (Paredit)" :operation (on-caret paredit/backward-sexp) :keyboard-shortcut (shortcut "ctrl alt OPEN_BRACKET")}
   {:name "backward-select" :text "Select backward" :description "Select backward a sexpr (Paredit)" :operation (on-selection paredit/backward-select) :keyboard-shortcut (shortcut "ctrl shift alt OPEN_BRACKET")}
   {:name "wrap-round" :text "Wrap with ()" :description "Wrap the form or selection with () (Paredit)" :operation (wrap-with \() :keyboard-shortcut (shortcut "ctrl shift 9" "meta shift 9")}
   {:name "wrap-square" :text "Wrap with []" :description "Wrap the form or selection with [] (Paredit)" :operation (wrap-with \[) :keyboard-shortcut (shortcut "ctrl OPEN_BRACKET" "meta OPEN_BRACKET")}
   {:name "wrap-curly" :text "Wrap with {}" :description "Wrap the form or selection with {} (Paredit)" :operation (wrap-with \{) :keyboard-shortcut (shortcut "ctrl shift OPEN_BRACKET" "meta shift OPEN_BRACKET")}
   {:name "splice-sexp" :text "Splice sexpr" :description "Remove the delimiters of the current sexpr (Paredit)" :operation (on-caret paredit/splice) :keyboard-shortcut (shortcut "alt S")}
   {:name "splice-sexp-killing-backward" :text "Splice sexpr killing backward" :description "Kill the forms before the caret and splice the current sexpr (Paredit)" :operation (on-caret paredit/splice-killing-backward)}
   {:name "splice-sexp-killing-forward" :text "Splice sexpr killing forward" :description "Kill the forms after the caret and splice the current sexpr (Paredit)" :operation (on-caret paredit/splice-killing-forward)}
   {:name "split-sexp" :text "Split sexpr" :description "Split the current sexpr or string at the caret (Paredit)" :operation (on-caret paredit/split) :keyboard-shortcut (shortcut "alt shift S" "ctrl alt S")}
   {:name "join-sexps" :text "Join sexprs" :description "Join the sexprs or strings around the caret (Paredit)" :operation (on-caret paredit/join) :keyboard-shortcut (shortcut "ctrl shift S" "ctrl alt J")}
   {:name "kill-line" :text "Kill to end of line" :description "Kill to the end of the line or the current sexpr, keeping balance (Paredit)" :operation (on-caret paredit/kill) :keyboard-shortcut (shortcut "alt D" "ctrl K")}
   {:name "backward-up" :text "Move backward out of sexpr" :description "Move the caret to the start of the current sexpr (Paredit)" :operation (on-caret paredit/backward-up)}
   {:name "forward-up" :text "Move forward out of sexpr" :description "Move the caret past the end of the current sexpr (Paredit)" :operation (on-caret paredit/forward-up)}
   {:name "forward-down" :text "Move forward into sexpr" :description "Move the caret into the next sexpr (Paredit)" :operation (on-caret paredit/forward-down)}
   {:name "backward-down" :text "Move backward into sexpr" :description "Move the caret into the end of the previous sexpr (Paredit)" :operation (on-caret paredit/backward-down)}])

(defn perform-action! [operation ^AnActionEvent event]
  (when-let [editor ^Editor (.getData event CommonDataKeys/EDITOR_EVEN_IF_INACTIVE)]
    (when (clojure-editor? editor)
      (when-let [result (operation editor)]
        (WriteCommandAction/runWriteCommandAction
         (.getProject event)
         ^Runnable (fn [] (apply-result! editor result)))))))

(defn refresh-widgets!
  "Updates the Paredit status bar widget of every open project."
  []
  (doseq [project (db/all-projects)]
    (some-> (.getStatusBar (WindowManager/getInstance) ^Project project)
            (.updateWidget widget-id))))

(defn toggle-strict-mode! [^Project project]
  (let [enabled? (not (settings/paredit-enabled?))]
    (settings/set-paredit-enabled! enabled?)
    (refresh-widgets!)
    (notification/show-notification!
     {:project project
      :type :info
      :title "Paredit"
      :message (if enabled?
                 "Structural editing mode is on: brackets stay balanced while typing."
                 "Structural editing mode is off.")})))
