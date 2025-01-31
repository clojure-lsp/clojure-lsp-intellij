(ns com.github.clojure-lsp.intellij.extension.brace-matcher
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.BraceMatcher
   :implements [com.intellij.lang.PairedBraceMatcher])
  (:import
   [com.github.clojure_lsp.intellij ClojureTokens]
   [com.intellij.lang BracePair]
   [com.intellij.psi.tree IElementType]))

(set! *warn-on-reflection* true)

(defn -getPairs [_]
  (into-array BracePair ClojureTokens/BRACE_PAIRS))

(defn -isPairedBracesAllowedBeforeType
  [_ _ ^IElementType context-type]
  (boolean context-type))

(defn -getCodeConstructStart [_ _psi-file opening-brace-offset]
  opening-brace-offset)
