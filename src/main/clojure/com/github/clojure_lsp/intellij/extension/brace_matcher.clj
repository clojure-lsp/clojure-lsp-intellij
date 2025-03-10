(ns com.github.clojure-lsp.intellij.extension.brace-matcher
  (:require
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]])
  (:import
   [com.github.clojure_lsp.intellij ClojureTokens]
   [com.intellij.lang BracePair PairedBraceMatcher]
   [com.intellij.psi.tree IElementType]))

(set! *warn-on-reflection* true)

(def-extension BraceMatcher []
  PairedBraceMatcher
  (getPairs [_]
    (into-array BracePair ClojureTokens/BRACE_PAIRS))
  (isPairedBracesAllowedBeforeType
    [_ _ ^IElementType context-type]
    (boolean context-type))
  (getCodeConstructStart [_ _psi-file opening-brace-offset]
    opening-brace-offset))
