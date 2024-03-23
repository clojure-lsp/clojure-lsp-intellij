(ns com.github.clojure-lsp.intellij.extension.file-manager
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.FileDocumentManagerListener
   :implements [com.intellij.openapi.fileEditor.FileDocumentManagerListener])
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.editor :as editor])
  (:import
   [com.intellij.openapi.editor Document]
   [com.intellij.openapi.fileEditor FileDocumentManager]))

(defn -beforeAllDocumentsSaving [_])
(defn -beforeAnyDocumentSaving [_ _ _])
(defn -beforeFileContentReload [_ _ _])
(defn -fileWithNoDocumentChanged [_ _])
(defn -fileContentReloaded [_ _ _])
(defn -fileContentLoaded [_ _ _])
(defn -unsavedDocumentDropped [_ _])
(defn -unsavedDocumentsDropped [_])
(defn -afterDocumentUnbound [_ _ _])

(defn -beforeDocumentSaving [_ ^Document document]
  (let [vfile (.getFile (FileDocumentManager/getInstance) document)]
    (when-let [client (some-> vfile editor/v-file->project lsp-client/connected-client)]
      (lsp-client/notify! client [:textDocument/didSave
                                  {:textDocument {:uri (.getUrl vfile)}}]))))
