(ns com.github.clojure-lsp.intellij.extension.annotator
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.Annotator
   :extends com.intellij.lang.annotation.ExternalAnnotator)
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor])
  (:import
   (com.intellij.lang.annotation AnnotationHolder HighlightSeverity)
   [com.intellij.openapi.editor Document]
   (com.intellij.psi PsiDocumentManager PsiFile)))

(set! *warn-on-reflection* true)

(defn ^:private severity->highlight-severity [^long severity]
  (case severity
    1 HighlightSeverity/ERROR
    2 HighlightSeverity/WARNING
    3 HighlightSeverity/INFO))

(defn -collectInformation
  ([_ psi-file] psi-file)
  ([_ psi-file _ _] psi-file))

(defn -doAnnotate [_ ^PsiFile psi-file]
  (when-let [virtual-file (.getVirtualFile psi-file)]
    (db/get-in (.getProject psi-file) [:diagnostics (.getUrl virtual-file)])))

(defn -apply [_ ^PsiFile psi-file result ^AnnotationHolder holder]
  (when-let [document ^Document (some-> (PsiDocumentManager/getInstance (.getProject psi-file))
                                        (.getDocument psi-file))]
    (doseq [{:keys [range message code severity _source]} result]
      (-> holder
          (.newAnnotation (severity->highlight-severity severity) (str message " [" code "]"))
          (.range (editor/range->text-range range document))
          (.create)))))
