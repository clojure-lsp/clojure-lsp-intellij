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
   [com.intellij.psi PsiDocumentManager PsiElement PsiFile]
   [com.intellij.openapi.fileTypes SyntaxHighlighterFactory]
   [com.intellij.openapi.editor.colors EditorColorsManager]
   [com.github.clojure_lsp.intellij ClojureLanguage]
   [java.awt Color]))

(set! *warn-on-reflection* true)

(defn ^:private rgb-html-style [^Color color]
  (str "rgb(" (.getRed color) ", " (.getGreen color) ", " (.getBlue color) ")"))

(defn ^:private html-style [prop value]
  (if (some? value)
    (str prop ":" value ";") ""))

(defn ^:private highlight-html-text
  [^String text {:keys [foreground-color background-color font-type]}]
  (let [color-style       (html-style "color" (when foreground-color
                                                (rgb-html-style foreground-color)))
        background-style  (html-style "background-color" (when background-color
                                                           (rgb-html-style foreground-color)))
        font-weight-style (html-style "font-weight" (case font-type
                                                      :bold "bold"
                                                      :bold-italic "bold"
                                                      "normal"))
        font-style        (html-style "font-style" (case font-type
                                                     :italic "italic"
                                                     :bold-italic "italic"
                                                     "normal"))]
    (str "<span style=\"" color-style background-style font-weight-style font-style "\">" text "</span>")))

(defn ^:private highlight-html-code [^String code]
  (let [highlighter  (SyntaxHighlighterFactory/getSyntaxHighlighter (ClojureLanguage/INSTANCE) nil nil)
        lexer        (.getHighlightingLexer highlighter)
        color-scheme (.getGlobalScheme (EditorColorsManager/getInstance))]
    (loop [highlighted-code (str)]
      (if (empty? highlighted-code)
        (.start lexer code)
        (.advance lexer))
      (let [lexer-not-finished? (some? (.getTokenType lexer))]
        (if lexer-not-finished?
          (let [text             (.getTokenText lexer)
                highlight        (first (.getTokenHighlights highlighter (.getTokenType lexer)))
                highlight-attrs  (.getAttributes color-scheme highlight)
                foreground-color (some-> highlight-attrs .getForegroundColor)
                background-color (some-> highlight-attrs .getBackgroundColor)
                font-type        (some-> highlight-attrs .getFontType (#(case (int %)
                                                                         1 :bold
                                                                         2 :italic
                                                                         3 :bold-italic
                                                                         nil)))]
            (if (some some? [foreground-color background-color font-type])
              (recur (str highlighted-code (highlight-html-text text {:foreground-color foreground-color
                                                                      :background-color background-color
                                                                      :font-type        font-type})))
              (recur (str highlighted-code text))))
          highlighted-code)))))

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

        (when-let [html (markdown/md-to-html-string (:value contents)
                                                    :codeblock-no-escape? true
                                                    :codeblock-callback (fn [code language]
                                                                          (if (= language "clojure")
                                                                            (highlight-html-code code)
                                                                            code)))]
          (-> (HtmlBuilder.)
              (.appendRaw html)
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
