(ns com.github.clojure-lsp.intellij.extension.syntax-highlighter
  (:require
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]])
  (:import
   [com.github.clojure_lsp.intellij ClojureLanguage]
   [com.github.clojure_lsp.intellij.language ClojureSyntaxHighlighter]
   [com.intellij.openapi.fileTypes SyntaxHighlighterFactory]))

(def-extension ClojureSyntaxHighlighter []
  SyntaxHighlighterFactory
  (getSyntaxHighlighter [_ _ _]
    (ClojureSyntaxHighlighter. (ClojureLanguage/INSTANCE))))
