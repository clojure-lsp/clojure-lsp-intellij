(ns com.github.clojure-lsp.intellij.extension.rename
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.RenameHandler
   :implements [com.intellij.refactoring.rename.RenameHandler])
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.file-system :as file-system]
   [com.github.ericdallo.clj4intellij.util :as util])
  (:import
   [com.intellij.openapi.actionSystem CommonDataKeys DataContext]
   [com.intellij.openapi.editor Editor]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.ui Messages NonEmptyInputValidator]
   [com.intellij.openapi.util TextRange]
   [com.intellij.psi PsiDocumentManager PsiFile]))

(set! *warn-on-reflection* true)

(defn -isAvailableOnDataContext [_ ^DataContext data-context]
  (boolean
   (and (.getData data-context CommonDataKeys/PROJECT)
        (.getData data-context CommonDataKeys/EDITOR)
        (.getPsiFile (PsiDocumentManager/getInstance (.getData data-context CommonDataKeys/PROJECT))
                     (.getDocument ^Editor (.getData data-context CommonDataKeys/EDITOR))))))

(defn -isRenaming [this data-context]
  (-isAvailableOnDataContext this data-context))

(defn ^:private prepare-rename-current-name [client ^Editor editor line character]
  (let [document (.getDocument editor)
        project (.getProject editor)
        response @(lsp-client/request! client [:textDocument/prepareRename
                                               {:text-document {:uri (editor/editor->uri editor)}
                                                :position {:line line
                                                           :character character}}])]
    (if-let [{:keys [message]} (:error response)]
      (lsp-client/show-message {:project project} {:type 1 :message message})
      (let [start ^int (editor/document+position->offset (:start response) document)
            end ^int (editor/document+position->offset (:end response) document)]
        (.getText document (TextRange. start end))))))

(defn -invoke
  ;; rename a file outside editor
  ([_this ^Project _project ^"[Lcom.intellij.psi.PsiElement;" _elements ^DataContext _data-context])
  ;; rename a element in a editor
  ([_ ^Project project ^Editor editor ^PsiFile _psi-file ^DataContext _data-context]
   (when-let [client (lsp-client/connected-server project)]
     (let [[line character] (util/editor->cursor-position editor)]
       (when-let [current-name ^String (prepare-rename-current-name client editor line character)]
         (when-let [new-name (Messages/showInputDialog project "Enter new name: " "Rename" (Messages/getQuestionIcon) current-name (NonEmptyInputValidator.))]
           (->> @(lsp-client/request! client [:textDocument/rename
                                              {:text-document {:uri (editor/editor->uri editor)}
                                               :position {:line line
                                                          :character character}
                                               :new-name new-name}])
                (file-system/apply-workspace-edit project "LSP Rename" true))))))))
