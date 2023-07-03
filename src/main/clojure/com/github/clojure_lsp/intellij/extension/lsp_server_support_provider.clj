(ns com.github.clojure-lsp.intellij.extension.lsp-server-support-provider
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.LspServerSupportProvider
   :implements [com.intellij.platform.lsp.api.LspServerSupportProvider])
  (:import
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.vfs VirtualFile]
   [com.intellij.platform.lsp.api LspServerSupportProvider$LspServerStarter])
  (:require
    [com.github.clojure-lsp.intellij.logger :as logger]))

(defn -fileOpened [^Project project ^VirtualFile file ^LspServerSupportProvider$LspServerStarter server-starter]
  (logger/info "----------------->"))
