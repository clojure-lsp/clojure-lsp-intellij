(ns com.github.clojure-lsp.intellij.extension.semantic-token-provider
  (:require
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]])
  (:import
   [com.intellij.openapi.editor.colors TextAttributesKey]
   [com.intellij.psi PsiFile]
   [com.redhat.devtools.lsp4ij.features.semanticTokens DefaultSemanticTokensColorsProvider SemanticTokensColorsProvider SemanticTokensHighlightingColors]
   [java.awt Font]
   [java.util List]
   [org.eclipse.lsp4j SemanticTokenModifiers SemanticTokenTypes]))

(set! *warn-on-reflection* true)

(def ^:private default-provider (DefaultSemanticTokensColorsProvider.))

(defn ^:private modifier? [modifier token-modifiers]
  (some #(= modifier %) token-modifiers))

(defn ^:private make-bold [^TextAttributesKey text-attribute-key]
  (TextAttributesKey/createTextAttributesKey
   (str (.getExternalName text-attribute-key) "_BOLD")
   (doto (.clone (.getDefaultAttributes text-attribute-key))
     (.setFontType Font/BOLD))))

(defn ^:private bold-function-declaration* []
  (make-bold SemanticTokensHighlightingColors/FUNCTION_DECLARATION))

(def ^:private bold-function-declaration (memoize bold-function-declaration*))

(def-extension ClojureSemanticTokensColorsProvider []
  SemanticTokensColorsProvider
  (getTextAttributesKey [_ ^String token-type ^List token-modifiers ^PsiFile psi-file]
    (condp = token-type
      SemanticTokenTypes/Namespace SemanticTokensHighlightingColors/STATIC_PROPERTY
      SemanticTokenTypes/Function (if (modifier? SemanticTokenModifiers/Definition token-modifiers)
                                    (bold-function-declaration)
                                    SemanticTokensHighlightingColors/MACRO)
      SemanticTokenTypes/Type SemanticTokensHighlightingColors/STATIC_PROPERTY
      SemanticTokenTypes/Variable SemanticTokensHighlightingColors/READONLY_VARIABLE
      SemanticTokenTypes/Keyword SemanticTokensHighlightingColors/NUMBER
      (.getTextAttributesKey ^DefaultSemanticTokensColorsProvider default-provider token-type token-modifiers psi-file))))
