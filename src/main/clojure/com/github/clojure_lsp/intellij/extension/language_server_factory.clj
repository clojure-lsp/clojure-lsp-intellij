(ns com.github.clojure-lsp.intellij.extension.language-server-factory
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.LanguageServerFactory
   :implements [com.redhat.devtools.lsp4ij.LanguageServerFactory])
  (:require
   [com.github.clojure-lsp.intellij.server :as server]
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [com.github.ericdallo.clj4intellij.tasks :as tasks]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.intellij.execution.configurations GeneralCommandLine]
   [com.intellij.openapi.project Project ProjectLocator]
   [com.intellij.openapi.vfs VirtualFile]
   [com.redhat.devtools.lsp4ij.client LanguageClientImpl]
   [com.redhat.devtools.lsp4ij.client.features LSPClientFeatures]
   [com.redhat.devtools.lsp4ij.server OSProcessStreamConnectionProvider]
   [org.eclipse.lsp4j InitializeParams]
   [org.eclipse.lsp4j.services LanguageServer]))

(set! *warn-on-reflection* true)

(defn -createConnectionProvider [_ ^Project project]
  (doto (proxy+
         []
         OSProcessStreamConnectionProvider)
    (.setCommandLine (GeneralCommandLine. ["/home/greg/dev/clojure-lsp/clojure-lsp" "listen"]))))

(defn -createLanguageClient [_ ^Project project]
  (LanguageClientImpl. project))

;; TODO custom server methods
(defn -getServerInterface [_] LanguageServer)

(defonce ^:private server (atom {:status :not-found}))

(defn ^:private install-server [project]
  (swap! server assoc :status :installing)
  (tasks/run-background-task!
   project
   "Installing clojure-lsp"
   (fn [indicator]
     (tasks/set-progress indicator "Clojure LSP: downloading server")
     (Thread/sleep 5000) ;; Simulate download server

     (swap! server assoc :status :installed)
     (server/start-server! project))))

;; TODO client features
(defn -createClientFeatures [_]
  (proxy+ [] LSPClientFeatures
    (isEnabled [_this ^VirtualFile file]
      (logger/info "-----> isEnabled" @server)
      (case (:status @server)
        :installing
        false

        :installed
        true

        :not-found
        (do (install-server (.guessProjectForFile (ProjectLocator/getInstance) file))
            false)))
    (initializeParams [_ ^InitializeParams params]
      (.setWorkDoneToken params "clojure-lsp-startup")
      (.setInitializationOptions params {"dependency-scheme" "jar"
                                         "hover" {"arity-on-same-line?" true}}))))
