(ns com.github.clojure-lsp.intellij.extension.new-file
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.ide.actions CreateFileFromTemplateAction CreateFileFromTemplateDialog$Builder]
   [com.intellij.ide.fileTemplates FileTemplate]
   [com.intellij.psi PsiDirectory]))

(set! *warn-on-reflection* true)

(defn ^:private filename->source-path [filename project]
  (let [source-paths (get-in (lsp-client/server-info project) [:final-settings "source-paths"])]
    (first (filter #(string/starts-with? filename %) source-paths))))

(defn ^:private dir->partial-namespace
  [filename project]
  (when-let [current-source-path (filename->source-path filename project)]
    (some-> filename
            (string/replace-first (re-pattern current-source-path) "")
            (string/replace (System/getProperty "file.separator") ".")
            (string/replace #"_" "-")
            not-empty
            (subs 1)
            (str "."))))

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
  (let [dir-filename (.getPath (.getVirtualFile dir))
        separator (System/getProperty "file.separator")
        source-path (filename->source-path dir-filename project)
        ns-path (-> ns
                    (string/replace "." separator)
                    (string/replace "-" "_"))
        new-name (string/replace-first (.getCanonicalPath (io/file source-path ns-path))
                                       (str dir-filename separator)
                                       "")]
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
