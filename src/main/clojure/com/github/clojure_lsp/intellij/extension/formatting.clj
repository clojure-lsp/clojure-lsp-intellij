(ns com.github.clojure-lsp.intellij.extension.formatting
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.Formatting
   :extends com.intellij.formatting.service.AsyncDocumentFormattingService)
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.client :as lsp-client])
  (:import
   [com.intellij.formatting.service AsyncDocumentFormattingService$FormattingTask AsyncFormattingRequest]
   [com.intellij.psi PsiFile]
   [com.github.clojure_lsp.intellij ClojureFileType]))

(set! *warn-on-reflection* true)

(defn -getFeatures [_]
  ;; TODO add other features, like range formatting, organizeImports
  #{})

(defn -canFormat [_ ^PsiFile psi-file]
  (and (identical? :connected (:status @db/db*))
       (instance? ClojureFileType (.getFileType psi-file))))

(defn -getName [_]
  "LSP format")

(defn -getNotificationGroupId [_]
  "LSP format")

(defn -createFormattingTask [_ ^AsyncFormattingRequest request]
  (let [context (.getContext request)
        client (:client @db/db*)
        file (.getContainingFile context)
        uri (.getUrl (.getVirtualFile file))]
    (reify AsyncDocumentFormattingService$FormattingTask
      (run [_]
        (try
          (let [[{:keys [new-text]}] @(lsp-client/request! client [:textDocument/formatting
                                                                   {:text-document {:uri uri}}])]
            (if new-text
              (.onTextReady request new-text)
              (.onTextReady request (.getDocumentText request))))
          (catch Exception e
            (.onError request "LSP format error" (.getMessage e)))))
      (cancel [_] true)
      (isRunUnderProgress [_] true))))
