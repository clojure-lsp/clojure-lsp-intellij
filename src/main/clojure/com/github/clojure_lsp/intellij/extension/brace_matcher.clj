(ns com.github.clojure-lsp.intellij.extension.brace-matcher
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.BraceMatcher
   :implements [com.intellij.lang.PairedBraceMatcher])
  (:import
   [com.github.clojure_lsp.intellij ClojureTokens]
   [com.github.clojure_lsp.intellij.language.psi ClojureTypes]
   [com.intellij.lang BracePair]
   [com.intellij.psi.tree IElementType]))

(set! *warn-on-reflection* true)

(defn -getPairs [_]
  (into-array BracePair ClojureTokens/BRACE_PAIRS))

(defn -isPairedBracesAllowedBeforeType
  [_ _ ^IElementType context-type]
  (or (not context-type)
      (.contains ClojureTokens/WHITESPACES context-type)
      (.contains ClojureTokens/COMMENTS context-type)
      (= ClojureTypes/C_COMMA context-type)
      (= ClojureTypes/C_PAREN2 context-type)
      (= ClojureTypes/C_BRACE2 context-type)
      (= ClojureTypes/C_BRACKET2 context-type)))

(defn -getCodeConstructStart [_ _psi-file opening-brace-offset]
  opening-brace-offset)
