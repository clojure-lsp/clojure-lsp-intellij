(ns com.github.clojure-lsp.intellij.extension.syntax-highlighter
  (:gen-class
   :name com.github.clojure-lsp.intellij.extension.SyntaxHighlighter
   :extends com.intellij.openapi.fileTypes.SyntaxHighlighterFactory)
  (:import
   [com.github.clojure_lsp.intellij ClojureLanguage]
   [org.intellij.clojure.lang ClojureSyntaxHighlighter]))

(defn -getSyntaxHighlighter [_ _ _]
  (ClojureSyntaxHighlighter. (ClojureLanguage/INSTANCE)))
