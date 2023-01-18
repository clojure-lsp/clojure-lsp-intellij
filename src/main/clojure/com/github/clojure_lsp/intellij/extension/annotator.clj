(ns com.github.clojure-lsp.intellij.extension.annotator
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.Annotator
   :extends com.intellij.lang.annotation.ExternalAnnotator)
  (:require
   [com.github.clojure-lsp.intellij.db :as db])
  (:import
   (com.intellij.lang.annotation AnnotationHolder HighlightSeverity)
   [com.intellij.openapi.editor Document]
   (com.intellij.openapi.util TextRange)
   (com.intellij.psi PsiDocumentManager PsiFile)))

(set! *warn-on-reflection* true)

(defn ^:private severity->highlight-severity [^long severity]
  (case severity
    1 HighlightSeverity/ERROR
    2 HighlightSeverity/WARNING
    3 HighlightSeverity/INFO))

(defn ^:private position->point [{:keys [line character]} ^Document document]
  (if (and (<= 0 line)
           (< line (.getLineCount document)))
    (let [start-line (.getLineStartOffset document line)
          end-line (.getLineEndOffset document line)]
      (loop [column 0
             offset start-line]
        (if (and (< offset end-line)
                 (< column character))
          (recur (inc column) (inc offset))
          offset)))
    (.getTextLength document)))

(defn ^:private range->text-range ^TextRange [range ^Document document]
  (TextRange/create (position->point (:start range) document)
                    (position->point (:end range) document)))

(defn -collectInformation
  ([_ psi-file] psi-file)
  ([_ psi-file _ _] psi-file))

(defn -doAnnotate [_ ^PsiFile psi-file]
  (when-let [virtual-file (.getVirtualFile psi-file)]
    (get-in @db/db* [:diagnostics (.getUrl virtual-file)])))

(defn -apply [_ ^PsiFile psi-file result ^AnnotationHolder holder]
  (when-let [document ^Document (some-> (PsiDocumentManager/getInstance (.getProject psi-file))
                                        (.getDocument psi-file))]
    (doseq [{:keys [range message code severity _source]} result]
      (-> holder
          (.newAnnotation (severity->highlight-severity severity) (str message " [" code "]"))
          (.range (range->text-range range document))
          (.create)))))
