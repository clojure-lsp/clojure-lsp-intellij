(ns com.github.clojure-lsp.intellij.file-system
  (:require
   [clojure.java.io :as io]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.config :as config]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [com.github.ericdallo.clj4intellij.util :as util]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.intellij.openapi.fileEditor
    FileDocumentManager]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.vfs LocalFileSystem VfsUtil VirtualFile]
   [java.net URI]))

(set! *warn-on-reflection* true)

(defn create-temp-file ^VirtualFile
  [^Project project ^String path ^String text]
  (let [temp-file (io/file (config/project-cache-path project) path)]
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

(defn ^:private create-document
  [{:keys [uri]}]
  (let [f (io/file (URI. uri))
        parent-vfile (VfsUtil/createDirectories (.getAbsolutePath (.getParentFile f)))]
    (.findOrCreateChildData parent-vfile nil (.getName f))))

(declare apply-workspace-edit-sync)

(defn will-rename-file [project old-uri new-uri]
  (let [client (lsp-client/connected-server project)]
    (->> (lsp-client/request! client [:workspace/willRenameFiles
                                      {:files [{:old-uri old-uri
                                                :new-uri new-uri}]}])
         deref
         (apply-workspace-edit-sync project false))))

(defn ^:private rename-document
  [{:keys [old-uri new-uri]} project]
  (will-rename-file project old-uri new-uri)
  (let [old-file (io/file (URI. old-uri))
        new-file (io/file (URI. new-uri))
        vfile (util/uri->v-file old-uri)]
    (.rename vfile nil (.getName new-file))
    (when-not (= (.getAbsolutePath (.getParentFile old-file))
                 (.getAbsolutePath (.getParentFile new-file)))
      (.move vfile nil (util/uri->v-file (str (.toURI (.getParentFile new-file))))))))

(defn ^:private apply-document-change
  [{{:keys [uri]} :text-document :keys [edits]} project move-caret?]
  (let [editor (util/uri->editor uri project false)
        document (.getDocument editor)
        sorted-edits (sort-by (comp #(editor/document+position->offset % document) :start :range) > edits)]
    (doseq [{:keys [new-text range]} sorted-edits
            :let [start (editor/document+position->offset (:start range) document)
                  end (editor/document+position->offset (:end range) document)]]
      (cond
        (>= end 0)
        (if (<= (- end start) 0)
          (.insertString document start new-text)
          (.replaceString document start end new-text))

        (= 0 start)
        (.setText document new-text)

        (> start 0)
        (.insertString document start new-text)

        :else
        (.insertString document (.getTextLength document) (str "\n" new-text)))
      (when move-caret?
        (.moveToOffset (.getCaretModel editor)
                       (+ (count new-text) start))))
    (.saveDocument (FileDocumentManager/getInstance) document)))

(defn ^:private apply-workspace-edit-sync [project move-caret? {:keys [document-changes]}]
  (doseq [document-change document-changes]
    (case (:kind document-change)
      "create" (create-document document-change)
      "rename" (rename-document document-change project)
      (apply-document-change document-change project move-caret?))))

(defn apply-workspace-edit ^Boolean
  [^Project project label move-caret? edit]
  ;; TODO Handle more resourceOperations like renaming and deleting files
  ;; TODO Improve to check version to known if file changed
  (app-manager/invoke-later!
   {:invoke-fn
    (fn []
      (app-manager/write-action!
       {:run-fn
        (fn []
          (app-manager/execute-command!
           {:name label
            :project project
            :command-fn
            (fn []
              (apply-workspace-edit-sync project move-caret? edit))}))}))}))
