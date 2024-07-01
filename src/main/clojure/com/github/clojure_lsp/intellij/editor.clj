(ns com.github.clojure-lsp.intellij.editor
  (:require
   [clojure-lsp.shared :as lsp.shared]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.ericdallo.clj4intellij.util :as util])
  (:import
   [com.intellij.openapi.editor Document Editor]
   [com.intellij.openapi.fileEditor
    FileDocumentManager]
   [com.intellij.openapi.project Project ProjectLocator]
   [com.intellij.openapi.util TextRange]
   [com.intellij.openapi.util.text StringUtil]
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

(defn document+position->offset ^Integer [{:keys [line character]} ^Document document]
  (position->offset (.getText document) line character))

(defn range->text-range ^TextRange [range ^Document document]
  (TextRange/create (document+position->offset (:start range) document)
                    (document+position->offset (:end range) document)))

(defn text-range->range [^TextRange range ^Editor editor]
  {:start (offset->cursor-position editor (.getStartOffset range))
   :end (offset->cursor-position editor (.getEndOffset range))})

(defn uri->psi-file ^PsiFile [^String uri ^Project project]
  (.findFile (PsiManager/getInstance project)
             (util/uri->v-file uri)))

(defn editor->uri [^Editor editor]
  ;; TODO sanitize URL, encode, etc
  (.getUrl (.getFile (FileDocumentManager/getInstance) (.getDocument editor))))

(defn filename->project-relative-filename [filename ^Project project]
  (lsp.shared/relativize-filepath
   filename
   (.getBasePath project)))

(defn virtual->psi-file ^PsiFile [^VirtualFile v-file ^Project project]
  (.findFile (PsiManager/getInstance project) v-file))

(defn v-file->project ^Project [^VirtualFile v-file]
  (.guessProjectForFile (ProjectLocator/getInstance)
                        v-file))
