(ns com.github.clojure-lsp.intellij.listener.file
  (:gen-class
   :name com.github.clojure_lsp.intellij.listener.FileListener
   :implements [com.intellij.openapi.fileEditor.FileEditorManagerListener
                com.intellij.openapi.editor.event.DocumentListener])
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.project-lsp :as project]
   [com.github.clojure-lsp.intellij.server :as server])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]
   [com.intellij.openapi.editor.event DocumentEvent]
   [com.intellij.openapi.fileEditor FileDocumentManager FileEditorManager]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.vfs VirtualFile]))

(set! *warn-on-reflection* true)

(def ^:private valid-extensions #{"clj" "cljs" "cljc" "cljd" "edn" "bb" "clj_kondo"})

(defn ^:private ensure-server-up!
  "If server was not started before, check if it's a Clojure project and starts it."
  [^VirtualFile file ^Project project]
  (cond
    (and (not (db/empty-db?))
         (contains? #{:connecting :connected} (db/get-in project [:status])))
    true

    (not (contains? valid-extensions (.getExtension file)))
    false

    (not (project/clojure-project? project))
    false

    :else
    (do
      (db/init-db-for-project project)
      (db/load-settings-from-state! project (SettingsState/get))
      (server/start-server! project))))

(defn -fileOpened [_this ^FileEditorManager source ^VirtualFile file]
  (let [project (.getProject source)]
    (when (ensure-server-up! file project)
      (db/await-field
       project
       :client
       (fn [client]
         (when (contains? valid-extensions (.getExtension file))
           (let [url (.getUrl file)
                 text (slurp (.getInputStream file))]
             (lsp-client/notify!
              client
              [:textDocument/didOpen
               {:text-document {:uri url
                                :language-id "clojure"
                                :version 0
                                :text text}}])
             (db/assoc-in project [:documents url] {:version 0
                                                    :text text}))))))))

(defn -fileClosed [_ ^FileEditorManager file-editor-manager ^VirtualFile file]
  (let [project (.getProject file-editor-manager)]
    (db/await-field
     project
     :client
     (fn [client]
       (let [url (.getUrl file)
             documents (db/get-in project [:documents])]
         (when (get documents url)
           (lsp-client/notify!
            client
            [:textDocument/didClose
             {:text-document {:uri url}}])
           (db/assoc-in project [:documents] (dissoc documents url))))))))

(defn -fileOpenedSync [_ _ _ _])
(defn -selectionChanged [_ _])

(defn -beforeDocumentChange [_ _])
(defn -bulkUpdateStarting [_ _])
(defn -bulkUpdateFinished [_ _])

(defn -documentChanged [_ ^DocumentEvent event]
  (let [file-document-manager (FileDocumentManager/getInstance)]
    (when-let [vfile (.getFile file-document-manager (.getDocument event))]
      (when-let [project (editor/v-file->project vfile)]
        (when-let [client (lsp-client/connected-client project)]
          (let [url (.getUrl vfile)]
            (when-let [{:keys [version]} (db/get-in project [:documents url])]
              (lsp-client/notify!
               client
               [:textDocument/didChange
                {:text-document {:uri url
                                 :version (inc version)}
                 :content-changes [{:text (.getText (.getDocument event))}]}])
              (db/update-in project [:documents url :version] inc))))))))
