(ns com.github.clojure-lsp.intellij.workspace-edit
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.file-system :as file-system]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [com.github.ericdallo.clj4intellij.util :as util]))

(set! *warn-on-reflection* true)

(defmethod lsp-client/workspace-apply-edit :default [{:keys [project]} {:keys [label edit]}]
  (file-system/apply-workspace-edit project label false edit)
  {:applied true})

(defmethod lsp-client/show-document :default [{:keys [project]} {:keys [uri take-focus selection]}]
  (app-manager/invoke-later!
   {:invoke-fn (fn []
                 (let [editor (util/uri->editor uri project (boolean take-focus))]
                   (.moveToOffset (.getCaretModel editor)
                                  (editor/document+position->offset (:start selection) (.getDocument editor)))))})
  {:done true})
