(ns com.github.clojure-lsp.intellij.extension.syntax-highlighter
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.SyntaxHighlighter
   :extends com.intellij.openapi.fileTypes.SyntaxHighlighterFactory)
  (:import
   [com.github.clojure_lsp.intellij ClojureLanguage]
   [com.github.clojure_lsp.intellij.language ClojureSyntaxHighlighter]))

(defn -getSyntaxHighlighter [_ _ _]
  (ClojureSyntaxHighlighter. (ClojureLanguage/INSTANCE)))
