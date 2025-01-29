(ns com.github.clojure-lsp.intellij.editor
  (:require
   [com.github.clojure-lsp.intellij.editor :as editor])
  (:import
   [com.intellij.openapi.editor Editor]
   [com.intellij.openapi.fileEditor FileDocumentManager]
   [com.intellij.openapi.util.text StringUtil]))

(set! *warn-on-reflection* true)

(defn editor->uri [^Editor editor]
  ;; TODO sanitize URL, encode, etc
  (.getUrl (.getFile (FileDocumentManager/getInstance) (.getDocument editor))))

(defn offset->cursor-position [^Editor editor offset]
  (let [text (.getCharsSequence (.getDocument editor))
        line-col (StringUtil/offsetToLineColumn text offset)]
    [(.line line-col) (.column line-col)]))
