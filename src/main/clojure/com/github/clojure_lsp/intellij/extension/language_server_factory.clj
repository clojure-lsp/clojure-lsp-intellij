(ns com.github.clojure-lsp.intellij.extension.language-server-factory
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.LanguageServerFactory
   :implements [com.redhat.devtools.lsp4ij.LanguageServerFactory])
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.config :as config]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.server :as server]
   [com.github.clojure-lsp.intellij.settings :as settings]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.intellij.execution.configurations GeneralCommandLine]
   [com.intellij.openapi.progress ProgressIndicator]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.vfs LocalFileSystem VirtualFile]
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
                      (or (settings/server-path)
                          (some-> ^File (:path @server) .getCanonicalPath)
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

(defn ^:private create-temp-file ^VirtualFile
  [^Project project ^String path ^String text]
  (let [temp-file (io/file (config/project-cache-path project) path)]
    (io/make-parents temp-file)
    (spit temp-file text)
    (proxy+ [] VirtualFile
      (getName [_] (.getName temp-file))
      (getFileSystem [_] (LocalFileSystem/getInstance))
      (getPath [_] (.getCanonicalPath temp-file))
      (isWritable [_] false)
      (isDirectory [_] false)
      (isValid [_] true)
      (getParent [_] nil)
      (getChildren [_] [])
      (contentsToByteArray [_] (.getBytes text))
      (getTimeStamp [_] 0)
      (getLength [_] (count (.getBytes text)))
      (refresh [_ _ _ _])
      (getInputStream [_] (io/input-stream temp-file))
      (getOutputStream [_ _ _ _] (io/output-stream temp-file))
      (getModificationStamp [_] -1))))

(defn ^:private find-file-by-uri [^String uri]
  (if (and (string/starts-with? uri "file:")
           (string/includes? uri ".jar!"))
    (let [fixed-uri (string/replace-first uri "file:" "jar:file:")
          old-vfile (LSPIJUtils/findResourceFor fixed-uri)
          project (editor/guess-project-for old-vfile)
          dependency-contents (lsp-client/dependency-contents fixed-uri project)
          jar-pattern (re-pattern (str "^(jar|zip):(file:.+)!" (System/getProperty "file.separator") "(.+)"))
          path (last (re-find jar-pattern fixed-uri))
          _tmp-file (create-temp-file project path dependency-contents)]
      ;; TODO fix support for clojure/dependencyContents
      #_tmp-file
      (LSPIJUtils/findResourceFor fixed-uri))
    (LSPIJUtils/findResourceFor uri)))

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
         (do (install-server (editor/guess-project-for file))
             false)))
     (initializeParams [_ ^InitializeParams params]
       (.setWorkDoneToken params "clojure-lsp-startup")
       (.setInitializationOptions params {"dependency-scheme" "jar"
                                          "hover" {"arity-on-same-line?" true}}))
     (findFileByUri ^VirtualFile [_ ^String uri]
       (find-file-by-uri uri)))
    (.setProgressFeature (proxy+ [] LSPProgressFeature
                           (updateMessage [_ ^String message ^ProgressIndicator indicator]
                             (.setText indicator (str "LSP: " message)))))))
