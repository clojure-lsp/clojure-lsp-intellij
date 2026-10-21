(ns com.github.clojure-lsp.intellij.editor
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager])
  (:import
   [com.intellij.openapi.editor CaretModel Editor LogicalPosition]
   [com.intellij.openapi.fileEditor FileDocumentManager]
   [com.intellij.openapi.project ProjectLocator]
   [com.intellij.openapi.util.text StringUtil]
   [com.intellij.openapi.vfs VirtualFile]
   [com.redhat.devtools.lsp4ij LSPIJUtils]))

(set! *warn-on-reflection* true)

(defn editor->uri
  "The file URI of the editor's document, built the same way LSP4IJ builds
   the URIs it sends to the server, e.g. file:///C:/Users/Some%20One/foo.clj.
   VirtualFile.getUrl is not a valid URI on Windows (file://C:/...) and does
   not encode spaces, so the server could not match the document."
  [^Editor editor]
  (LSPIJUtils/toUriAsString (.getFile (FileDocumentManager/getInstance) (.getDocument editor))))

(defn offset->cursor-position [^Editor editor offset]
  (let [text (.getCharsSequence (.getDocument editor))
        line-col (StringUtil/offsetToLineColumn text offset)]
    [(.line line-col) (.column line-col)]))

(defn guess-project-for [^VirtualFile file]
  (or (.guessProjectForFile (ProjectLocator/getInstance) file)
      (first (db/all-projects))))

(defn move-caret-to-position
  "Moves the caret to the specified logical position in the editor."
  [^Editor editor line column]
  (let [caret ^CaretModel (.getCaretModel editor)
        new-position (LogicalPosition. line column)]
    @(app-manager/invoke-later!
      {:invoke-fn (fn [] (.moveToLogicalPosition caret new-position))})))
