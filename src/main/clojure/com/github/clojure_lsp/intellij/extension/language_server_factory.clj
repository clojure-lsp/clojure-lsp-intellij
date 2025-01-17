(ns com.github.clojure-lsp.intellij.extension.language-server-factory
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.LanguageServerFactory
   :implements [com.redhat.devtools.lsp4ij.LanguageServerFactory])
  (:require
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.intellij.execution.configurations GeneralCommandLine]
   [com.intellij.openapi.project Project]
   [com.redhat.devtools.lsp4ij.server OSProcessStreamConnectionProvider]))

(defn -createConnectionProvider [_ ^Project project]
  (logger/info "------->")
  (doto (proxy+
          []
          OSProcessStreamConnectionProvider)
    (.setCommandLine (GeneralCommandLine. ["/home/greg/dev/clojure-lsp/clojure-lsp" "listen"]))))

;; TODO custom commands
;; (defn -createLanguageClient [_ ^Project project]
;;   )

;; TODO customer server methods
;; (defn -getServerInterface [_]
;;   )
