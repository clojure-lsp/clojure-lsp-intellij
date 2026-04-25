(ns com.github.clojure-lsp.intellij.extension.new-file
  (:require
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.ide.actions CreateFileFromTemplateAction CreateFileFromTemplateDialog$Builder]
   [com.intellij.ide.fileTemplates FileTemplate]
   [com.intellij.psi PsiDirectory]
   [java.io File]
   [java.nio.file Path Paths]))

(set! *warn-on-reflection* true)

(defn ^:private ->path ^Path [^String s]
  (when (and s (not (string/blank? s)))
    (let [cleaned (cond-> s
                    (string/starts-with? s "file://") (subs 7))]
      (try
        (-> (Paths/get cleaned (into-array String []))
            .toAbsolutePath
            .normalize)
        (catch Exception _ nil)))))

(defn ^:private filename->source-path [filename project]
  (let [dir-path (->path filename)
        source-paths (get-in (lsp-client/server-info project) [:final-settings "source-paths"])]
    (when dir-path
      (some (fn [sp]
              (when-let [spath (->path sp)]
                (when (.startsWith dir-path spath)
                  spath)))
            source-paths))))

(defn ^:private dir->partial-namespace
  [filename project]
  (when-let [source-path (filename->source-path filename project)]
    (when-let [dir-path (->path filename)]
      (let [rel-str (str (.relativize ^Path source-path ^Path dir-path))]
        (when-not (string/blank? rel-str)
          (-> rel-str
              (string/replace File/separator ".")
              (string/replace "_" "-")
              (str ".")))))))

(defn ^:private ns->rel-path
  "Возвращает путь нового файла ОТНОСИТЕЛЬНО dir, как требует createFileFromTemplate."
  [project ^String ns ^PsiDirectory dir]
  (let [separator    File/separator
        dir-filename (.getPath (.getVirtualFile dir))
        source-path  (filename->source-path dir-filename project)
        ns-path      (-> ns
                         (string/replace "." separator)
                         (string/replace "-" "_"))]
    (if-let [sp source-path]
      (if-let [dir-path (->path dir-filename)]
        (let [rel-dir (str (.relativize ^Path sp ^Path dir-path))
              prefix  (if (string/blank? rel-dir) "" (str rel-dir separator))]
          (if (and (seq prefix) (string/starts-with? ns-path prefix))
            (subs ns-path (count prefix))
            ns-path))
        ns-path)
      ns-path)))

(defn ^:private dialog [project ^PsiDirectory dir ^CreateFileFromTemplateDialog$Builder builder]
  (let [filename (.getPath (.getVirtualFile dir))
        namespace (dir->partial-namespace filename project)]
    (-> builder
        (.setTitle "New Clojure namespace")
        (.setDefaultText (or namespace ""))
        (.addKind "Clojure (.clj)" Icons/CLOJURE "ClojureNamespace")
        (.addKind "ClojureScript (.cljs)" Icons/CLOJURE_SCRIPT "ClojureScriptNamespace")
        (.addKind "CLJC (.cljc)" Icons/CLOJURE "CLJCNamespace")
        (.addKind "ClojureDart (.cljd)" Icons/CLOJURE_DART "ClojureDartNamespace"))))

(defn ^:private create-file-from-template
  [project ^String ns ^FileTemplate template ^PsiDirectory dir]
  (let [new-name (ns->rel-path project ns dir)]
    (CreateFileFromTemplateAction/createFileFromTemplate new-name template dir nil true)))

(defn ->ClojureNewFileAction [project]
  (proxy+
   ["Clojure namespace" "Create a new Clojure namespace" Icons/CLOJURE]
   CreateFileFromTemplateAction
    (buildDialog [_ project dir builder]
      (dialog project dir builder))
    (getActionName [_ _dir new-name _template-name]
      (str "Create Clojure namespace: " new-name))
    (createFileFromTemplate [_ name template dir]
      (create-file-from-template project name template dir))))
