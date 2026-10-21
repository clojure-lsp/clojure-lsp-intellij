(ns com.github.clojure-lsp.intellij.extension.qualified-name-provider
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]])
  (:import
   [com.github.clojure_lsp.intellij ClojureLanguage]
   [com.intellij.ide.actions QualifiedNameProvider]
   [com.intellij.openapi.editor Document]
   [com.intellij.openapi.vfs VirtualFile]
   [com.intellij.psi PsiDocumentManager PsiElement PsiFile]
   [com.redhat.devtools.lsp4ij LSPIJUtils]))

(set! *warn-on-reflection* true)

(def ^:private var-buckets #{"var-usages" "var-definitions"})

;; Copy Reference calls providers on the EDT.
(def ^:private timeout-ms 1000)

(defn ^:private element->qualified-name
  "Qualified name of a clj-kondo analysis element returned by clojure/cursorInfo/raw;
   nil for locals, keywords and anything else that is not a var or namespace."
  [{:strs [element definition]}]
  (let [{:strs [bucket name ns to]} element]
    (cond
      (and (= "var-usages" bucket) to) (str to "/" name)
      (= "var-definitions" bucket) (str ns "/" name)
      (contains? var-buckets (get definition "bucket")) (str (get definition "ns") "/" (get definition "name"))
      (#{"namespace-definitions" "namespace-usages"} bucket) (str name))))

(defn qualified-name
  "Asks clojure-lsp for the fully qualified name of the var or namespace at `element`."
  [^PsiElement element]
  (when-let [^PsiFile file (when-not (instance? PsiFile element)
                             (.getContainingFile element))]
    (when-let [^VirtualFile vfile (when (identical? ClojureLanguage/INSTANCE (.getLanguage file))
                                    (.getVirtualFile file))]
      (let [project (.getProject element)
            ^Document document (.getDocument (PsiDocumentManager/getInstance project) file)
            offset (.getTextOffset element)]
        (when (and document (<= 0 offset (.getTextLength document)))
          (let [line (.getLineNumber document offset)
                character (- offset (.getLineStartOffset document line))]
            (some element->qualified-name
                  (get (lsp-client/cursor-info (LSPIJUtils/toUriAsString vfile) line character project timeout-ms)
                       "elements"))))))))

(def-extension ClojureQualifiedNameProvider []
  QualifiedNameProvider
  (adjustElementToCopy [_ _element] nil)
  (getQualifiedName [_ ^PsiElement element] (qualified-name element))
  (qualifiedNameToElement [_ _fqn _project] nil))
