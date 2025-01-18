(ns com.github.clojure-lsp.intellij.extension.language-server-factory
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.LanguageServerFactory
   :implements [com.redhat.devtools.lsp4ij.LanguageServerFactory])
  (:require
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.intellij.execution.configurations GeneralCommandLine]
   [com.intellij.openapi.project Project]
   [com.redhat.devtools.lsp4ij.client LanguageClientImpl]
   [com.redhat.devtools.lsp4ij.client.features LSPClientFeatures]
   [com.redhat.devtools.lsp4ij.server OSProcessStreamConnectionProvider]
   [org.eclipse.lsp4j.services LanguageServer]))

(defn -createConnectionProvider [_ ^Project project]
  (doto (proxy+
         []
         OSProcessStreamConnectionProvider)
    (.setCommandLine (GeneralCommandLine. ["/home/greg/dev/clojure-lsp/clojure-lsp" "listen"]))))

;; TODO custom commands
(defn -createLanguageClient [_ ^Project project]
  (LanguageClientImpl. project))

;; TODO custom server methods
(defn -getServerInterface [_] LanguageServer)

;; TODO client features
(defn -createClientFeatures [_]
  (LSPClientFeatures.))
