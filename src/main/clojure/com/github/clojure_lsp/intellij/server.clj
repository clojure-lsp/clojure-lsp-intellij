(ns com.github.clojure-lsp.intellij.server
  (:require
   [babashka.process :as p]
   [clojure.core.async :as async]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.config :as config]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.notification :as notification]
   [com.github.clojure-lsp.intellij.project-lsp :as project]
   [com.github.clojure-lsp.intellij.tasks :as tasks]
   [com.github.clojure-lsp.intellij.workspace-edit]
   [com.github.ericdallo.clj4intellij.logger :as logger])
  (:import
   [com.intellij.openapi.progress ProgressIndicator]
   [com.intellij.openapi.project Project]
   [com.intellij.util EnvironmentUtil]
   [java.io File]
   [java.util.zip ZipInputStream]))

(set! *warn-on-reflection* true)

(def ^:private client-capabilities
  {:text-document {:hover {:content-format ["markdown"]}}
   :workspace {:workspace-edit {:document-changes true}}})

(def ^:private artifacts
  {:linux {:amd64 "clojure-lsp-native-static-linux-amd64.zip"
           :aarch64 "clojure-lsp-native-linux-aarch64.zip"}
   :macos {:amd64 "clojure-lsp-native-macos-amd64.zip"
           :aarch64 "clojure-lsp-native-macos-aarch64.zip"}
   :windows {:amd64 "clojure-lsp-native-windows-amd64.zip"}})

(defn ^:private clean-up-server []
  (when (p/alive? (:server-process @db/db*))
    (p/destroy-tree (:server-process @db/db*)))
  (swap! db/db* assoc :status :disconnected
         :client nil
         :server-process nil
         :diagnostics {})
  (run! #(% :disconnected) (:on-status-changed-fns @db/db*)))

(defn ^:private os-name []
  (let [os-name (string/lower-case (System/getProperty "os.name" "generic"))]
    (cond
      (string/starts-with? os-name "windows") :windows
      (string/starts-with? os-name "mac") :macos
      :else :linux)))

(def ^:private latest-version-uri
  "https://raw.githubusercontent.com/clojure-lsp/clojure-lsp/master/lib/resources/CLOJURE_LSP_RELEASED_VERSION")

(def ^:private download-artifact-uri
  "https://github.com/clojure-lsp/clojure-lsp/releases/download/%s/%s")

(defn ^:private unzip-file [input ^File dest-file]
  (with-open [stream (-> input io/input-stream ZipInputStream.)]
    (loop [entry (.getNextEntry stream)]
      (when entry
        (if (.isDirectory entry)
          (when-not (.exists dest-file)
            (.mkdirs dest-file))
          (clojure.java.io/copy stream dest-file))
        (recur (.getNextEntry stream))))))

(defn ^:private download-server! [indicator ^File download-path]
  (tasks/set-progress indicator "LSP: Downloading clojure-lsp")
  (let [version (string/trim (slurp latest-version-uri))
        platform (os-name)
        arch (keyword (System/getProperty "os.arch"))
        artifact-name (get-in artifacts [platform arch])
        uri (format download-artifact-uri version artifact-name)
        dest-file download-path
        dest-path (.getCanonicalPath dest-file)]
    (logger/info "Downloading clojure-lsp from" uri)
    (unzip-file (io/input-stream uri) dest-file)
    (doto (io/file dest-file)
      (.setWritable true)
      (.setReadable true)
      (.setExecutable true))
    (swap! db/db* assoc :downloaded-server-path dest-path)
    (logger/info "Downloaded clojure-lsp to" dest-path)))

(defn ^:private spawn-server! [^Project project indicator server-path]
  (logger/info "Spawning LSP server process using path" server-path)
  (tasks/set-progress indicator "LSP: Starting server...")
  (let [process (p/process [server-path "listen"]
                           {:dir (.getBasePath project)
                            :env (EnvironmentUtil/getEnvironmentMap)
                            :err :string})
        client (lsp-client/client (:in process) (:out process))]
    (async/go
      (try
        (p/check process)
        (catch Exception e
          (logger/warn "Error on clojure-lsp process:\n" (pr-str e))
          (clean-up-server)
          (.cancel ^ProgressIndicator indicator))))
    (swap! db/db* assoc
           :server-process process
           :client client)
    (lsp-client/start-client! client {:progress-indicator indicator})
    (tasks/set-progress indicator "LSP: Initializing...")
    (let [request-initiatilize (lsp-client/request! client [:initialize
                                                            {:root-uri (project/project->root-uri project)
                                                             :work-done-token "lsp-startup"
                                                             :initialization-options (merge {:dependency-scheme "jar"
                                                                                             :hover {:arity-on-same-line? true}}
                                                                                            (:settings @db/db*))
                                                             :capabilities client-capabilities}])]
      (loop []
        (Thread/sleep 500)
        (cond
          (and (not (realized? request-initiatilize))
               (not (p/alive? process)))
          (notification/show-notification! {:type :error
                                            :title "Clojure LSP process error"
                                            :message @(:err process)})

          (and (realized? request-initiatilize)
               (p/alive? process))
          (lsp-client/notify! client [:initialized {}])

          :else
          (recur))))))

(defn start-server! [^Project project]
  (swap! db/db* assoc :status :connecting)
  (run! #(% :connecting) (:on-status-changed-fns @db/db*))
  (tasks/run-background-task!
   project
   "Clojure LSP startup"
   (fn [indicator]
     (let [db @db/db*
           download-path (config/download-server-path)
           custom-server-path (-> db :settings :server-path)]
       (cond
         custom-server-path
         (spawn-server! project indicator custom-server-path)

         (.exists download-path)
         (spawn-server! project indicator download-path)

         :else
         (do (download-server! indicator download-path)
             (spawn-server! project indicator download-path)))

       (swap! db/db* assoc :status :connected)
       (run! #(% :connected) (:on-status-changed-fns @db/db*))
        ;; For race conditions when server starts too fast
        ;; and other places that listen didn't setup yet
       (future
         (Thread/sleep 1000)
         (run! #(% :connected) (:on-status-changed-fns @db/db*)))
       (logger/info "Initialized LSP server"))))
  true)

(defn shutdown! []
  (when-let [client (lsp-client/connected-client)]
    @(lsp-client/request! client [:shutdown {}])
    (clean-up-server)))
