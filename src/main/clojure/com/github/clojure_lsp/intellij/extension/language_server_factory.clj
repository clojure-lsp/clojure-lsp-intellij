(ns com.github.clojure-lsp.intellij.extension.language-server-factory
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.LanguageServerFactory
   :implements [com.redhat.devtools.lsp4ij.LanguageServerFactory])
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.config :as config]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.server :as server]
   [com.github.clojure-lsp.intellij.settings :as settings]
   [com.github.ericdallo.clj4intellij.tasks :as tasks]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.intellij.execution.configurations GeneralCommandLine]
   [com.intellij.openapi.progress ProgressIndicator]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.vfs LocalFileSystem VirtualFile]
   [com.redhat.devtools.lsp4ij LSPIJUtils ServerStatus]
   [com.redhat.devtools.lsp4ij.client LanguageClientImpl]
   [com.redhat.devtools.lsp4ij.client.features EditorBehaviorFeature LSPClientFeatures LSPProgressFeature]
   [com.redhat.devtools.lsp4ij.installation LanguageServerInstallerBase]
   [com.redhat.devtools.lsp4ij.server OSProcessStreamConnectionProvider]
   [java.io File]
   [java.util List]
   [org.eclipse.lsp4j InitializeParams]))

(set! *warn-on-reflection* true)

(defonce ^:private server-path* (atom nil))
(defonce ^:private server-installing* (atom false))

(defn -createConnectionProvider [_ ^Project _project]
  (let [path (loop []
               (Thread/sleep 100)
               (or (settings/server-path)
                   (some-> ^File @server-path* .getCanonicalPath)
                   (recur)))
        command [path "listen"]]
    (doto (proxy+
           []
           OSProcessStreamConnectionProvider)
      (.setCommandLine (GeneralCommandLine. ^List command)))))

(defn -createLanguageClient [_ ^Project project]
  (LanguageClientImpl. project))

(defn -getServerInterface [_]
  com.github.clojure_lsp.intellij.ClojureLanguageServer)

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
  (let [lsp-client-features (proxy+ [] LSPClientFeatures
                              (keepServerAlive [_] true)
                              (initializeParams [_ ^InitializeParams params]
                                (.setWorkDoneToken params "clojure-lsp-startup")
                                (.setInitializationOptions params {"dependency-scheme" "jar"
                                                                   "hover" {"arity-on-same-line?" true}}))
                              (findFileByUri ^VirtualFile [_ ^String uri]
                                (find-file-by-uri uri))
                              (handleServerStatusChanged [^LSPClientFeatures this ^ServerStatus server-status]
                                (let [status (keyword (.toString server-status))]
                                  (db/assoc-in (.getProject this) [:status] status)
                                  (run! #(% status) (db/get-in (.getProject this) [:on-status-changed-fns])))))]
    (.setProgressFeature lsp-client-features
                         (proxy+ [] LSPProgressFeature
                           (updateMessage [_ ^String message ^ProgressIndicator indicator]
                             (.setText indicator (str "LSP: " message)))))
    (.setServerInstaller lsp-client-features
                         (proxy+ [] LanguageServerInstallerBase
                           (getInstallationTaskTitle [_] "LSP: installing clojure-lsp")
                           (progressCheckingServerInstalled [_ indicator] (tasks/set-progress indicator "LSP: checking for clojure-lsp"))
                           (progressInstallingServer [_ indicator] (tasks/set-progress indicator "LSP: downloading clojure-lsp"))
                           (checkServerInstalled [_ _indicator]
                             (let [{:keys [status path]} (server/server-install-status)]
                               (if (identical? :installed status)
                                 (do
                                   (when-not @server-path* (reset! server-path* path))
                                   true)
                                 false)))
                           (install [^LanguageServerInstallerBase this _indicator]
                             (when-not @server-installing*
                               (reset! server-installing* true)
                               (reset! server-path* (server/install-server! (.getProject (.getClientFeatures this))))
                               (reset! server-installing* false)))))
    (.setEditorBehaviorFeature lsp-client-features
                               (proxy+ [lsp-client-features] EditorBehaviorFeature
                                 (isEnableSemanticTokensFileViewProvider [_ _] true)))
    lsp-client-features))
