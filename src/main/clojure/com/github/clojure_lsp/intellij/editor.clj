(ns com.github.clojure-lsp.intellij.editor
  (:require
   [com.github.clojure-lsp.intellij.editor :as editor])
  (:import
   [com.intellij.openapi.editor Editor]
   [com.intellij.openapi.util.text StringUtil]))

(set! *warn-on-reflection* true)

(defn offset->cursor-position [^Editor editor offset]
  (let [text (.getCharsSequence (.getDocument editor))
        line-col (StringUtil/offsetToLineColumn text offset)]
    [(.line line-col) (.column line-col)]))
