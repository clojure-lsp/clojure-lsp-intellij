(ns com.github.clojure-lsp.intellij.editor
  (:require
   [clojure-lsp.shared :as lsp.shared]
   [clojure.java.io :as io]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager])
  (:import
   [com.intellij.openapi.editor Document Editor]
   [com.intellij.openapi.fileEditor
    FileDocumentManager
    FileEditorManager
    OpenFileDescriptor
    TextEditor]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.util TextRange]
   [com.intellij.openapi.util.text StringUtil]
   [com.intellij.openapi.vfs LocalFileSystem]
   [com.intellij.openapi.vfs VirtualFile]
   [com.intellij.psi PsiFile]
   [com.intellij.psi PsiManager]))

(set! *warn-on-reflection* true)

(defn offset->cursor-position [^Editor editor offset]
  (let [text (.getCharsSequence (.getDocument editor))
        line-col (StringUtil/offsetToLineColumn text offset)]
    [(.line line-col) (.column line-col)]))

(defn position->offset [text line character]
  (StringUtil/lineColToOffset text line character))

(defn editor->cursor-position [^Editor editor]
  (let [offset (.. editor getCaretModel getCurrentCaret getOffset)]
    (offset->cursor-position editor offset)))

(defn position->point [{:keys [line character]} ^Document document]
  (if (and (<= 0 line)
           (< line (.getLineCount document)))
    (let [start-line (.getLineStartOffset document line)
          end-line (.getLineEndOffset document line)]
      (loop [column 0
             offset start-line]
        (if (and (< offset end-line)
                 (< column character))
          (recur (inc column) (inc offset))
          offset)))
    (.getTextLength document)))

(defn range->text-range ^TextRange [range ^Document document]
  (TextRange/create (position->point (:start range) document)
                    (position->point (:end range) document)))

(defn text-range->range [^TextRange range ^Editor editor]
  {:start (offset->cursor-position editor (.getStartOffset range))
   :end (offset->cursor-position editor (.getEndOffset range))})

(defn uri->v-file ^VirtualFile [^String uri]
  (.findFileByIoFile (LocalFileSystem/getInstance)
                     (io/file (lsp.shared/uri->filename uri))))

(defn uri->psi-file ^PsiFile [^String uri ^Project project]
  (.findFile (PsiManager/getInstance project)
             (uri->v-file uri)))

(defn v-file->editor ^Editor [^VirtualFile v-file ^Project project on-open-close-file?]
  (let [file-manager (FileEditorManager/getInstance project)
        editor (if (.isFileOpen file-manager v-file)
                 (.getEditor ^TextEditor (first (.getAllEditors file-manager v-file)))
                 (.openTextEditor file-manager (OpenFileDescriptor. project v-file) false))]
    editor))

(defn uri->editor ^Editor [^String uri ^Project project on-open-close-file?]
  (let [v-file (uri->v-file uri)]
    (v-file->editor v-file project on-open-close-file?)))

(defn editor->uri [^Editor editor]
  ;; TODO sanitize URL, encode, etc
  (.getUrl (.getFile (FileDocumentManager/getInstance) (.getDocument editor))))

(defn virtual->psi-file ^PsiFile [^VirtualFile v-file ^Project project]
  (.findFile (PsiManager/getInstance project) v-file))

(defn apply-workspace-edit ^Boolean
  [^Project project label move-caret? {:keys [document-changes]}]
  ;; TODO Handle resourceOperations like creating, renaming and deleting files
  ;; TODO Improve to check version to known if file changed
  (app-manager/invoke-later!
   {:invoke-fn
    (fn []
      (app-manager/write-action!
       {:run-fn
        (fn []
          (app-manager/execute-command!
           {:name label
            :project project
            :command-fn
            (fn []
              (doseq [{{:keys [uri]} :text-document
                       :keys [edits]} document-changes
                      :let [editor (uri->editor uri project true)
                            document (.getDocument editor)
                            sorted-edits (sort-by (comp #(position->point % document) :start :range) > edits)]]
                (doseq [{:keys [new-text range]} sorted-edits
                        :let [start (position->point (:start range) document)
                              end (position->point (:end range) document)]]
                  (cond
                    (>= end 0)
                    (if (<= (- end start) 0)
                      (.insertString document start new-text)
                      (.replaceString document start end new-text))

                    (= 0 start)
                    (.setText document new-text)

                    (> start 0)
                    (.insertString document start new-text)

                    :else
                    nil)
                  (when move-caret?
                    (.moveToOffset (.getCaretModel editor)
                                   (+ (count new-text) start))))
                (.saveDocument (FileDocumentManager/getInstance) document)))}))}))}))
