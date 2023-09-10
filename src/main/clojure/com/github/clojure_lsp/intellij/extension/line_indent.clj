(ns com.github.clojure-lsp.intellij.extension.line-indent
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.LineIndentProvider
   :implements [com.intellij.psi.codeStyle.lineIndent.LineIndentProvider])
  (:require
   [clojure-lsp.parser :as lsp.parser]
   [clojure-lsp.refactor.edit :as lsp.edit]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [rewrite-clj.zip :as z])
  (:import
   [com.github.clojure_lsp.intellij ClojureLanguage]
   [com.intellij.openapi.editor Editor]))

(defn -isSuitableFor [_ language]
  (instance? ClojureLanguage language))

(defn -getLineIndent [_ _project ^Editor editor _language offset]
  ;; TODO move this logic to clojure-lsp, creating a custom request for that
  (let [document (.getDocument editor)
        [row col] (editor/offset->cursor-position editor offset)
        zloc (some-> (lsp.parser/safe-zloc-of-string (.getText document))
                     (lsp.parser/to-pos (inc row) col))]
    (when-let [parent-zloc (and zloc (z/up zloc))]
      (let [parent-col (:col (meta (z/node parent-zloc)))
            new-col (if (lsp.edit/top? zloc)
                      0
                      (if (not (identical? :list (z/tag parent-zloc)))
                        parent-col
                        (inc parent-col)))]
        (apply str (repeat new-col \space))))))
