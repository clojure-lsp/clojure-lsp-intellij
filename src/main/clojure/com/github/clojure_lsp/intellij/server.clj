(ns com.github.clojure-lsp.intellij.server
  (:require
   [babashka.process :as p]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.config :as config]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.notification :as notification]
   [com.github.clojure-lsp.intellij.project-lsp :as project]
   [com.github.clojure-lsp.intellij.workspace-edit]
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [com.github.ericdallo.clj4intellij.tasks :as tasks])
  (:import
   [com.github.ericdallo.clj4intellij ClojureClassLoader]
   [com.intellij.openapi.project Project]
   [com.intellij.util EnvironmentUtil]
   [java.io File]
   [java.util.zip ZipInputStream]))

(set! *warn-on-reflection* true)

(def ^:private client-capabilities
  {:text-document {:hover {:content-format ["markdown"]}
                   :implementation {}}
   :workspace {:workspace-edit {:document-changes true
                                :resource-operations ["create" "rename"]}
               :file-operations {:will-rename true}}
   :window {:show-document true}})

(def ^:private artifacts
  {:linux {:amd64 "clojure-lsp-native-static-linux-amd64.zip"
           :aarch64 "clojure-lsp-native-linux-aarch64.zip"}
   :macos {:amd64 "clojure-lsp-native-macos-amd64.zip"
           :aarch64 "clojure-lsp-native-macos-aarch64.zip"}
   :windows {:amd64 "clojure-lsp-native-windows-amd64.zip"}})

(defn ^:private clean-up-server [^Project project]
  (let [server-process (db/get-in project [:server-process])]
    (when (some-> server-process p/alive?)
      (p/destroy server-process)))
  (db/update-in project [] (fn [db]
                             (assoc db :status :disconnected
                                    :client nil
                                    :server-process nil
                                    :diagnostics {})))
  (run! #(% project :disconnected) (:on-status-changed-fns @db/db*)))

(defn ^:private os-name []
  (let [os-name (string/lower-case (System/getProperty "os.name" "generic"))]
    (cond
      (string/includes? os-name "win") :windows
      (string/includes? os-name "mac") :macos
      :else :linux)))

(defn ^:private os-arch []
  (if (= "aarch64" (System/getProperty "os.arch"))
    :aarch64
    :amd64))

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

(defn ^:private download-server! [project indicator ^File download-path ^File server-version-path latest-version]
  (tasks/set-progress indicator "LSP: Downloading clojure-lsp")
  (let [platform (os-name)
        arch (os-arch)
        artifact-name (get-in artifacts [platform arch])
        uri (format download-artifact-uri latest-version artifact-name)
        dest-server-file download-path
        dest-path (.getCanonicalPath dest-server-file)]
    (logger/info "Downloading clojure-lsp from" uri)
    (unzip-file (io/input-stream uri) dest-server-file)
    (doto (io/file dest-server-file)
      (.setWritable true)
      (.setReadable true)
      (.setExecutable true))
    (spit server-version-path latest-version)
    (db/assoc-in project [:downloaded-server-path] dest-path)
    (logger/info "Downloaded clojure-lsp to" dest-path)))

(defn ^:private spawn-server! [^Project project indicator server-path]
  (logger/info "Spawning LSP server process using path" server-path)
  (tasks/set-progress indicator "LSP: Starting server...")
  (let [trace-level (keyword (db/get-in project [:settings :trace-level]))
        process (p/process [server-path "listen"]
                           {:dir (.getBasePath project)
                            :env (EnvironmentUtil/getEnvironmentMap)
                            :err :string})
        client (lsp-client/client (:in process) (:out process) trace-level)]
    (db/assoc-in project [:server-process] process)
    (lsp-client/start-client! client {:progress-indicator indicator
                                      :project project})

    (tasks/set-progress indicator "LSP: Initializing...")
    (let [request-initiatilize (lsp-client/request! client [:initialize
                                                            {:root-uri (project/project->root-uri project)
                                                             :work-done-token "lsp-startup"
                                                             :initialization-options (merge {:dependency-scheme "jar"
                                                                                             :hover {:arity-on-same-line? true}}
                                                                                            (db/get-in project [:settings]))
                                                             :capabilities client-capabilities}])]
      (loop [count 0]
        (Thread/sleep 500)
        (cond
          (and (not (realized? request-initiatilize))
               (not (p/alive? process)))
          (notification/show-notification! {:project project
                                            :type :error
                                            :title "Clojure LSP process error"
                                            :message @(:err process)})

          (and (realized? request-initiatilize)
               (p/alive? process))
          (do (lsp-client/notify! client [:initialized {}])
              (db/assoc-in project [:client] client))

          :else
          (do
            (logger/info "Checking if server initialized, try number:" count)
            (recur (inc count))))))))

(defn start-server! [^Project project]
  (db/assoc-in project [:status] :connecting)
  (run! #(% project :connecting) (:on-status-changed-fns @db/db*))
  (tasks/run-background-task!
   project
   "Clojure LSP startup"
   (fn [indicator]
     (ClojureClassLoader/bind)
     (let [download-path (config/download-server-path)
           server-version-path (config/download-server-version-path)
           latest-version* (delay (try (string/trim (slurp latest-version-uri)) (catch Exception _ nil)))
           custom-server-path (db/get-in project [:settings :server-path])]
       (cond
         custom-server-path
         (spawn-server! project indicator custom-server-path)

         (and (.exists download-path)
              (or (not @latest-version*) ;; on network connection issues we use any downloaded server
                  (= (try (slurp server-version-path) (catch Exception _ :error-checking-local-version))
                     @latest-version*)))
         (spawn-server! project indicator download-path)

         @latest-version*
         (do (download-server! project indicator download-path server-version-path @latest-version*)
             (spawn-server! project indicator download-path))

         :else
         (notification/show-notification! {:project project
                                           :type :error
                                           :title "Clojure LSP download error"
                                           :message "There is no server downloaded and there was a network issue to download the latest one"}))

       (db/assoc-in project [:status] :connected)
       (run! #(% project :connected) (:on-status-changed-fns @db/db*))
        ;; For race conditions when server starts too fast
        ;; and other places that listen didn't setup yet
       (future
         (Thread/sleep 1000)
         (run! #(% project :connected) (:on-status-changed-fns @db/db*)))
       (logger/info "Initialized LSP server"))))
  true)

(defn shutdown! [^Project project]
  (when-let [client (lsp-client/connected-client project)]
    (db/assoc-in project [:status] :shutting-down)
    @(lsp-client/request! client [:shutdown {}])
    (clean-up-server project)))
