(ns com.github.clojure-lsp.intellij.listener.file
  (:gen-class
   :name com.github.clojure_lsp.intellij.listener.FileListener
   :implements [com.intellij.openapi.fileEditor.FileEditorManagerListener
                com.intellij.openapi.editor.event.DocumentListener])
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.project-lsp :as project]
   [com.github.clojure-lsp.intellij.server :as server])
  (:import
   [com.intellij.openapi.editor.event DocumentEvent]
   [com.intellij.openapi.fileEditor FileDocumentManager FileEditorManager]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.vfs VirtualFile]))

(set! *warn-on-reflection* true)

(def ^:private valid-extensions #{"clj" "cljs" "cljc" "cljd" "edn" "bb" "clj_kondo"})

(:project @db/db*)

(defn ^:private ensure-server-up!
  "If server was not started before, check if it's a Clojure file and starts it."
  [^VirtualFile file ^Project project]
  (or (not (identical? :disconnected (:status @db/db*)))
      (and (or (contains? valid-extensions (.getExtension file))
               (project/clojure-project? project @db/db*))
           (do
             (swap! db/db* assoc :project project)
             (server/spawn-server! project)))))

(defn -fileOpened [_this ^FileEditorManager source ^VirtualFile file]
  (when (ensure-server-up! file (.getProject source))
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
                                                  :text text}))))))

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
