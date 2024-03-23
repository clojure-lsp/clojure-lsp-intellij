(ns com.github.clojure-lsp.intellij.extension.documentation
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.Documentation
   :implements [com.intellij.lang.documentation.DocumentationProvider])
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [markdown.core :as markdown])
  (:import
   [com.intellij.openapi.util.text StringUtil]
   [com.intellij.openapi.util.text HtmlBuilder]
   [com.intellij.psi PsiDocumentManager PsiElement PsiFile]))

(set! *warn-on-reflection* true)

(defn ^:private build-doc [^PsiElement element]
  (when-let [client (db/get-in (.getProject element) [:client])]
    (when-let [psi-file ^PsiFile (.getContainingFile element)]
      (let [project (.getProject element)
            document (.getDocument (PsiDocumentManager/getInstance project) psi-file)
            text (.getCharsSequence document)
            line-col (StringUtil/offsetToLineColumn text (.getTextOffset element))
            {:keys [contents]} @(lsp-client/request! client [:textDocument/hover
                                                             {:text-document {:uri (.getUrl (.getVirtualFile psi-file))}
                                                              :position {:line (.line line-col)
                                                                         :character (.column line-col)}}])]

        (when-let [raw-html (markdown/md-to-html-string (:value contents))]
          (-> (HtmlBuilder.)
              (.appendRaw raw-html)
              (.toString)))))))

(defn -getCustomDocumentationElement
  [_ _ _ context-element _]
  context-element)

(defn -generateDoc [_ element _]
  (build-doc element))

(defn -getQuickNavigateInfo [_ element _]
  (build-doc element))

(defn -generateHoverDoc [_ _ _])
(defn -getUrlFor [_ _ _])
(defn -generateRenderedDoc [_ _])
(defn -collectDocComments [_ _ _])
(defn -getLocalImageForElement [_ _ _])
(defn -getDocumentationElementForLookupItem [_ _ _ _])
(defn -getDocumentationElementForLink [_ _ _ _])
