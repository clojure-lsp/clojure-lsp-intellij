(ns com.github.clojure-lsp.intellij.server
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.config :as config]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.notification :as notification]
   [com.github.clojure-lsp.intellij.server :as server]
   [com.github.clojure-lsp.intellij.settings :as settings]
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [com.github.ericdallo.clj4intellij.tasks :as tasks])
  (:import
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.project Project]
   [com.redhat.devtools.lsp4ij LanguageServerManager]
   [java.io File]
   [java.util.zip ZipInputStream]))

(set! *warn-on-reflection* true)

(def ^:private latest-version-uri
  "https://raw.githubusercontent.com/clojure-lsp/clojure-lsp/master/lib/resources/CLOJURE_LSP_RELEASED_VERSION")

(def ^:private download-artifact-uri
  "https://github.com/clojure-lsp/clojure-lsp/releases/download/%s/%s")

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

(def ^:private artifacts
  {:linux {:amd64 "clojure-lsp-native-static-linux-amd64.zip"
           :aarch64 "clojure-lsp-native-linux-aarch64.zip"}
   :macos {:amd64 "clojure-lsp-native-macos-amd64.zip"
           :aarch64 "clojure-lsp-native-macos-aarch64.zip"}
   :windows {:amd64 "clojure-lsp-native-windows-amd64.zip"}})

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
  (tasks/set-progress indicator "LSP: downloading clojure-lsp")
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

(defn install-server [project installed-fn]
  (tasks/run-background-task!
   project
   "LSP: install clojure-lsp"
   (fn [indicator]
     (let [download-path (config/download-server-path)
           server-version-path (config/download-server-version-path)
           latest-version* (delay (try (string/trim (slurp latest-version-uri)) (catch Exception _ nil)))
           custom-server-path (settings/server-path)]
       (cond
         custom-server-path
         (installed-fn {:status :installed :path custom-server-path})

         (and (.exists download-path)
              (or (not @latest-version*) ;; on network connection issues we use any downloaded server
                  (= (try (slurp server-version-path) (catch Exception _ :error-checking-local-version))
                     @latest-version*)))
         (installed-fn {:status :installed :path download-path})

         @latest-version*
         (do (download-server! project indicator download-path server-version-path @latest-version*)
             (installed-fn {:status :installed :path download-path}))

         :else
         (notification/show-notification! {:project project
                                           :type :error
                                           :title "Clojure LSP download error"
                                           :message "There is no server downloaded and there was a network issue to download the latest one"}))))))

(defn start! [^Project project]
  (.start (LanguageServerManager/getInstance project) "clojure-lsp"))

(defn shutdown! [^Project project]
  (.stop (LanguageServerManager/getInstance project) "clojure-lsp"))
