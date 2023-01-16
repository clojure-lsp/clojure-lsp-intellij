(ns com.github.clojure-lsp.intellij.listener.file-listener
  (:gen-class
   :main false
   :name com.github.clojure_lsp.intellij.listener.FileListener
   :extends com.github.clojure_lsp.intellij.WithLoader
   :implements [com.intellij.openapi.fileEditor.FileEditorManagerListener
                com.intellij.openapi.vfs.newvfs.BulkFileListener])
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.lsp-client :as lsp-client])
  (:import
   [com.intellij.openapi.vfs VirtualFile]
   [com.intellij.openapi.vfs.newvfs.events VFileEvent]))

(def ^:private valid-extensions #{"clj" "cljs" "cljc" "cljd" "edn" "bb" "clj_kondo"})

(defn -fileOpened [_this _source ^VirtualFile file]
  (when (contains? valid-extensions (.getExtension file))
    (let [url (.getUrl file)
          text (slurp (.getInputStream file))]
      (lsp-client/notify!
       (:client @db/db*)
       [:textDocument/didOpen
        {:text-document {:uri url
                         :language-id "clojure"
                         :version 0
                         :text text}}])
      (swap! db/db* assoc-in [:documents url] {:version 0
                                               :text text}))))

(defn -fileClosed [_ _ ^VirtualFile file]
  (let [url (.getUrl file)
        {:keys [client documents]} @db/db*]
    (when (get documents url)
      (lsp-client/notify!
       client
       [:textDocument/didClose
        {:text-document {:uri url}}])
      (swap! db/db* update [:documents] #(dissoc % url)))))

(defn -fileOpenedSync [_ _ _ _])
(defn -selectionChanged [_ _])

(defn -before [_ _])
(defn -after [_ events]
  (doseq [^VFileEvent event events]
    (when-let [file (.getFile event)]
      (let [url (.getUrl file)
            {:keys [client documents]} @db/db*]
        (when-let [{:keys [version]} (get documents url)]
          (lsp-client/notify!
           client
           [:textDocument/didChange
            {:text-document {:uri url
                             :version (inc version)}
             :content-changes [{:text (slurp (.getInputStream file))}]}])
          (swap! db/db* update-in [:documents url :version] inc))))))
