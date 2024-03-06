(ns com.github.clojure-lsp.intellij.extension.definition
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.Definition
   :extends com.intellij.codeInsight.navigation.actions.GotoDeclarationHandlerBase)
  (:require
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.config :as config]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.file-system :as file-system]
   [com.github.clojure-lsp.intellij.psi :as psi])
  (:import
   [com.intellij.openapi.editor Editor]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.vfs VirtualFile]
   [com.intellij.psi PsiElement]
   [com.intellij.psi PsiElement]))

(set! *warn-on-reflection* true)

(defn ^:private definition->psi-element
  [^VirtualFile v-file ^Project project definition text]
  (let [{{:keys [start]} :range} definition
        text (or text (slurp (:uri definition)))
        offset (inc (editor/position->offset text (:line start) (:character start)))]
    (psi/->LSPPsiElement "" project (editor/virtual->psi-file v-file project) offset offset)))

(defn dependency-content [client uri project definition path]
  (let [text @(lsp-client/request! client [:clojure/dependencyContents {:uri uri}])]
    (when (string? text)
      (let [v-file (file-system/create-temp-file project path text)]
        (definition->psi-element v-file project definition text)))))

(defn -getGotoDeclarationTarget [_ ^PsiElement element ^Editor editor]
  (when-let [client (and (= :connected (:status @db/db*))
                         (:client @db/db*))]
    (let [[line character] (:start (editor/text-range->range (.getTextRange element) editor))
          project ^Project (.getProject editor)]
      (when-let [definition @(lsp-client/request! client [:textDocument/definition
                                                          {:text-document {:uri (editor/editor->uri editor)}
                                                           :position {:line line
                                                                      :character character}}])]
        (when-let [uri (:uri definition)]
          (if (string/starts-with? uri "jar:")
            (let [jar-pattern (re-pattern (str "^(jar|zip):(file:.+)!" (System/getProperty "file.separator") "(.+)"))]
              (dependency-content client uri project definition (last (re-find jar-pattern uri))))
            ;; TODO improve this
            (if-let [v-file (editor/uri->v-file uri)]
              (definition->psi-element v-file project definition nil)
              (dependency-content client uri project definition (string/replace-first uri (str "file://" (config/project-cache-path project)) "")))))))))
