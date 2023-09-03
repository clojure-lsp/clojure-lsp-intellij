(ns com.github.clojure-lsp.intellij.application-manager
  (:import
   [com.intellij.openapi.application ApplicationManager ModalityState]
   [com.intellij.openapi.command CommandProcessor]
   [com.intellij.openapi.command UndoConfirmationPolicy]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.util Computable]))

(defn invoke-later!
  ([invoke-fn] (invoke-later! (ModalityState/defaultModalityState) invoke-fn))
  ([modality-state invoke-fn]
   (let [p (promise)]
     (.invokeLater
      (ApplicationManager/getApplication)
      (fn []
        (deliver p (invoke-fn)))
      modality-state)
     p)))

(defn read-action! [run-fn]
  (let [p (promise)]
    (.runReadAction
     (ApplicationManager/getApplication)
     (reify Computable
       (compute [_]
         (let [result (run-fn)]
           (deliver p result)
           result))))
    p))

(defn write-action! [run-fn]
  (let [p (promise)]
    (.runWriteAction
     (ApplicationManager/getApplication)
     (reify Computable
       (compute [_]
         (let [result (run-fn)]
           (deliver p result)
           result))))
    p))

(defn execute-command! [^String name ^Project project command-fn]
  (let [p (promise)]
    (.executeCommand
     (CommandProcessor/getInstance)
     project
     (fn []
       (let [result (command-fn)]
         (deliver p result)
         result))
     name
     "Clojure LSP"
     UndoConfirmationPolicy/DEFAULT
     false)
    p))
