(ns com.github.clojure-lsp.intellij.listener.file
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.lsp-client :as lsp-client])
  (:import
   [com.intellij.openapi.editor.event DocumentEvent]
   [com.intellij.openapi.fileEditor FileDocumentManager]
   [com.intellij.openapi.vfs VirtualFile])
  (:gen-class
   :name com.github.clojure_lsp.intellij.listener.FileListener
   :implements [com.intellij.openapi.fileEditor.FileEditorManagerListener
                com.intellij.openapi.editor.event.DocumentListener]))
(def ^:private valid-extensions #{"clj" "cljs" "cljc" "cljd" "edn" "bb" "clj_kondo"})

(defn -fileOpened [_this _source ^VirtualFile file]
  (db/await-init
   :client
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
                                                :text text})))))

(defn -fileClosed [_ _ ^VirtualFile file]
  (db/await-init
   :client
   (let [url (.getUrl file)
         {:keys [documents]} @db/db*]
     (when (get documents url)
       (lsp-client/notify!
        (:client @db/db*)
        [:textDocument/didClose
         {:text-document {:uri url}}])
       (swap! db/db* update [:documents] #(dissoc % url))))))

(defn -fileOpenedSync [_ _ _ _])
(defn -selectionChanged [_ _])

(defn -beforeDocumentChange [_ _])
(defn -bulkUpdateStarting [_ _])
(defn -bulkUpdateFinished [_ _])
(defn -documentChanged [_ ^DocumentEvent event]
  (when-let [vfile (.getFile (FileDocumentManager/getInstance) (.getDocument event))]
    (let [url (.getUrl vfile)
          {:keys [client documents]} @db/db*]
      (when-let [{:keys [version]} (get documents url)]
        (lsp-client/notify!
         client
         [:textDocument/didChange
          {:text-document {:uri url
                           :version (inc version)}
           :content-changes [{:text (.getText (.getDocument event))}]}])
        (swap! db/db* update-in [:documents url :version] inc)))))
