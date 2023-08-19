(ns com.github.clojure-lsp.intellij.file-system
  (:require
   [clojure.java.io :as io])
  (:import
   [com.intellij.openapi.application ApplicationManager]
   [com.intellij.openapi.command CommandProcessor]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.util Computable]
   [com.intellij.openapi.vfs VirtualFile]
   [com.intellij.openapi.vfs VirtualFileManager]))

(set! *warn-on-reflection* true)

(defn create-temp-file ^VirtualFile
  [^Project project ^String path ^String text]
  (let [temp-file-path (str (com.intellij.openapi.application.PathManager/getPluginsPath) "/clojure-lsp/cache/" (.getName project) path)
        temp-file (io/file temp-file-path)
        virtual-file-manager (VirtualFileManager/getInstance)]
    (.executeCommand
     (CommandProcessor/getInstance)
     nil
     (reify Runnable
       (run [_]
         (.runWriteAction
          (ApplicationManager/getApplication)
          (reify Computable
            (compute [_]
              (io/make-parents temp-file-path)
              (let [dir (.findFileByNioPath virtual-file-manager (-> temp-file .getParentFile .toPath))
                    file (.createChildData dir nil (.getName temp-file))]
                (.setBinaryContent file (.getBytes text))
                true))))))
     nil
     nil)
    (.findFileByNioPath virtual-file-manager (.toPath temp-file))))
