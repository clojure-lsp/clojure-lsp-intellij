(ns com.github.clojure-lsp.intellij.tasks
  (:import
   [com.intellij.openapi.progress ProgressIndicator ProgressManager Task$Backgroundable]))

(set! *warn-on-reflection* true)

(defn run-background-task! [project title run-fn]
  (.run (ProgressManager/getInstance)
        (proxy [Task$Backgroundable] [project title]
          (run [^ProgressIndicator indicator]
            (run-fn indicator)))))

(defn set-progress
  ([^ProgressIndicator indicator text1]
   (.setText indicator text1))
  ([^ProgressIndicator indicator text1 percentage]
   (.setText indicator text1)
   (.setIndeterminate indicator false)
   (.setFraction indicator (double (/ percentage 100)))))
