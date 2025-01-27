(ns com.github.clojure-lsp.intellij.extension.language-server-factory
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.LanguageServerFactory
   :implements [com.redhat.devtools.lsp4ij.LanguageServerFactory])
  (:require
   [com.github.clojure-lsp.intellij.server :as server]
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.intellij.execution.configurations GeneralCommandLine]
   [com.intellij.openapi.project Project ProjectLocator]
   [com.intellij.openapi.vfs VirtualFile]
   [com.redhat.devtools.lsp4ij.client LanguageClientImpl]
   [com.redhat.devtools.lsp4ij.client.features LSPClientFeatures]
   [com.redhat.devtools.lsp4ij.server OSProcessStreamConnectionProvider]
   [java.io File]
   [java.util List]
   [org.eclipse.lsp4j InitializeParams]
   [org.eclipse.lsp4j.services LanguageServer]))

(set! *warn-on-reflection* true)

(defonce ^:private server (atom {:status :not-found
                                 :path nil}))

(defn -createConnectionProvider [_ ^Project _project]
  (logger/info "---> connection" (:path @server))
  (let [server-path (or (some-> ^File (:path @server)
                                (.getCanonicalPath))
                        "mocked-server-path")
        command [server-path "listen"]]
    (logger/info "--------> starting" command)
    (doto (proxy+
           []
           OSProcessStreamConnectionProvider)
      (.setCommandLine (GeneralCommandLine. ^List command)))))

(defn -createLanguageClient [_ ^Project project]
  (LanguageClientImpl. project))

;; TODO custom server methods
(defn -getServerInterface [_] LanguageServer)

(defn ^:private install-server [project]
  (swap! server assoc :status :installing)
  (server/install-server
   project
   (fn [{:keys [status path]}]
     (swap! server assoc :status status :path path)))
  (server/start! project))

;; TODO client features
(defn -createClientFeatures [_]
  (proxy+ [] LSPClientFeatures
    (isEnabled [_this ^VirtualFile file]
      (logger/info "---> checking isEnabled")
      (let [r (case (:status @server)
                :installing
                false

                :installed
                true

                :not-found
                (do (install-server (.guessProjectForFile (ProjectLocator/getInstance) file))
                    false))]
        (logger/info "-----> isEnabled result" r)
        r))
    (initializeParams [_ ^InitializeParams params]
      (.setWorkDoneToken params "clojure-lsp-startup")
      (.setInitializationOptions params {"dependency-scheme" "jar"
                                         "hover" {"arity-on-same-line?" true}}))))
