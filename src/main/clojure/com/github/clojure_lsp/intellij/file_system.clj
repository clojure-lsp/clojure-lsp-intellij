(ns com.github.clojure-lsp.intellij.file-system
  (:require
   [clojure.java.io :as io]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager])
  (:import
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.vfs VirtualFile]
   [com.intellij.openapi.vfs VirtualFileManager]))

(set! *warn-on-reflection* true)

(defn create-temp-file ^VirtualFile
  [^Project project ^String path ^String text]
  (let [temp-file-path (str (com.intellij.openapi.application.PathManager/getPluginsPath) "/clojure-lsp/cache/" (.getName project) path)
        temp-file (io/file temp-file-path)
        virtual-file-manager (VirtualFileManager/getInstance)]
    @(app-manager/write-action!
      {:run-fn
       (fn []
         @(app-manager/execute-command!
           {:name "Temp file"
            :group-id "Clojure LSP"
            :project project
            :command-fn (fn []
                          (io/make-parents temp-file-path)
                          (let [dir (.refreshAndFindFileByNioPath virtual-file-manager (-> temp-file .getParentFile .toPath))
                                file (.createChildData dir nil (.getName temp-file))]
                            (.setBinaryContent file (.getBytes text))
                            true))}))})
    (.findFileByNioPath virtual-file-manager (.toPath temp-file))))
