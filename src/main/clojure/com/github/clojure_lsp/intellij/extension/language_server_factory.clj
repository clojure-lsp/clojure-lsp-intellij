(ns com.github.clojure-lsp.intellij.extension.language-server-factory
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.LanguageServerFactory
   :implements [com.redhat.devtools.lsp4ij.LanguageServerFactory])
  (:require
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.server :as server]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.intellij.execution.configurations GeneralCommandLine]
   [com.intellij.openapi.progress ProgressIndicator]
   [com.intellij.openapi.project Project ProjectLocator]
   [com.intellij.openapi.vfs VirtualFile]
   [com.redhat.devtools.lsp4ij LSPIJUtils]
   [com.redhat.devtools.lsp4ij.client LanguageClientImpl]
   [com.redhat.devtools.lsp4ij.client.features LSPClientFeatures LSPProgressFeature]
   [com.redhat.devtools.lsp4ij.server OSProcessStreamConnectionProvider]
   [java.io File]
   [java.util List]
   [org.eclipse.lsp4j InitializeParams]))

(set! *warn-on-reflection* true)

(defonce ^:private server (atom {:status :not-found
                                 :path nil}))

(defn -createConnectionProvider [_ ^Project _project]
  (let [server-path (loop []
                      (Thread/sleep 100)
                      (or (some-> ^File (:path @server) .getCanonicalPath)
                          (recur)))
        command [server-path "listen"]]
    (doto (proxy+
           []
           OSProcessStreamConnectionProvider)
      (.setCommandLine (GeneralCommandLine. ^List command)))))

(defn -createLanguageClient [_ ^Project project]
  (LanguageClientImpl. project))

(defn -getServerInterface [_]
  com.github.clojure_lsp.intellij.ClojureLanguageServer)

(defn ^:private install-server [project]
  (swap! server assoc :status :installing)
  (server/install-server
   project
   (fn [{:keys [status path]}]
     (swap! server assoc :status status :path path)
     (server/start! project))))

(defn -createClientFeatures [_]
  (doto
   (proxy+ [] LSPClientFeatures
     (isEnabled [_this ^VirtualFile file]
       (case (:status @server)
         :installing
         false

         :installed
         true

         :not-found
         (do (install-server (or (.guessProjectForFile (ProjectLocator/getInstance) file)
                                 (first (db/all-projects))))
             false)))
     (initializeParams [_ ^InitializeParams params]
       (.setWorkDoneToken params "clojure-lsp-startup")
       (.setInitializationOptions params {"dependency-scheme" "jar"
                                          "hover" {"arity-on-same-line?" true}}))
     (findFileByUri [_ ^String uri]
       (if (and (string/starts-with? uri "file:")
                (string/includes? uri ".jar!"))
         (LSPIJUtils/findResourceFor (string/replace-first uri "file:" "jar:file:"))
         (LSPIJUtils/findResourceFor uri))))
    (.setProgressFeature (proxy+ [] LSPProgressFeature
                           (updateMessage [_ ^String message ^ProgressIndicator indicator]
                             (.setText indicator (str "LSP: " message)))))))
