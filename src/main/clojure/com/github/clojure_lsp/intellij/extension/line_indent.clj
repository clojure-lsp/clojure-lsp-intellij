(ns com.github.clojure-lsp.intellij.extension.line-indent
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.LineIndentProvider
   :implements [com.intellij.psi.codeStyle.lineIndent.LineIndentProvider])
  (:require
   [cljfmt.config :as cljfmt.config]
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
      (let [op (some->> zloc lsp.edit/find-op)
            first-arg-same-row (when (and op (z/right op))
                                 (= (:row (meta (z/node op)))
                                    (:row (meta (z/node (z/right op))))))
            parent-col (:col (meta (z/node parent-zloc)))
            new-col (cond
                      ;; no parent function/op
                      (lsp.edit/top? zloc)
                      0

                      ;; (let [a 1|])
                      (not (identical? :list (z/tag parent-zloc)))
                      parent-col

                      ;; (-> a|)
                      (and first-arg-same-row
                           (not (get (:indents cljfmt.config/default-config) (z/sexpr op))))
                      (dec (:col (meta (z/node (z/right op)))))

                      ;; (my-function|)
                      :else
                      (inc parent-col))]
        (apply str (repeat new-col \space))))))
