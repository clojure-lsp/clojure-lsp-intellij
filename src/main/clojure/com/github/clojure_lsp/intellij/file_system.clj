(ns com.github.clojure-lsp.intellij.file-system
  (:require
   [clojure.java.io :as io]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.vfs LocalFileSystem VirtualFile]
   [java.io File]))

(set! *warn-on-reflection* true)

(defn plugin-path ^File []
  (io/file (com.intellij.openapi.application.PathManager/getPluginsPath) "clojure-lsp"))

(defn project-cache-path ^File [^Project project]
  (io/file (plugin-path) "cache" (.getName project)))

(defn create-temp-file ^VirtualFile
  [^Project project ^String path ^String text]
  (let [temp-file (io/file (project-cache-path project) path)]
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
