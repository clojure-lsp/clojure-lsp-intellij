(ns com.github.clojure-lsp.intellij.extension.rename
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.RenameHandler
   :implements [com.intellij.refactoring.rename.RenameHandler])
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor])
  (:import
   [com.intellij.openapi.actionSystem CommonDataKeys DataContext]
   [com.intellij.openapi.editor Editor]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.ui Messages NonEmptyInputValidator]
   [com.intellij.openapi.util TextRange]
   [com.intellij.psi PsiFile]
   [com.intellij.refactoring.rename PsiElementRenameHandler]))

(set! *warn-on-reflection* true)

(defn -isAvailableOnDataContext [_ ^DataContext data-context]
  (boolean
   (and (.getData data-context CommonDataKeys/EDITOR)
        (.getData data-context CommonDataKeys/PSI_FILE)
        (let [element (PsiElementRenameHandler/getElement data-context)]
          (instance? PsiFile element)))))

(defn -isRenaming [this data-context]
  (-isAvailableOnDataContext this data-context))

(defn ^:private prepare-rename-current-name [client ^Editor editor line character]
  (let [document (.getDocument editor)
        response @(lsp-client/request! client [:textDocument/prepareRename
                                               {:text-document {:uri (editor/editor->uri editor)}
                                                :position {:line line
                                                           :character character}}])]
    (if-let [{:keys [message]} (:error response)]
      (lsp-client/show-message {:type 1 :message message})
      (let [start ^int (editor/position->point (:start response) document)
            end ^int (editor/position->point (:end response) document)]
        (.getText document (TextRange. start end))))))

(defn -invoke
  ([_ ^Project project ^"[Lcom.intellij.psi.PsiElement;" _elements ^DataContext data-context]
   ;; TODO handle if only single element and do inPlaceRename instead.
   (-invoke project (.getData data-context CommonDataKeys/EDITOR) (.getData data-context CommonDataKeys/PSI_FILE) data-context))
  ([_ ^Project project ^Editor editor ^PsiFile _psi-file ^DataContext _data-context]
   (when-let [client (:client @db/db*)]
     (let [[line character] (editor/editor->cursor-position editor)]
       (when-let [current-name ^String (prepare-rename-current-name client editor line character)]
         (when-let [new-name (Messages/showInputDialog project "Enter new name: " "Rename" (Messages/getQuestionIcon) current-name (NonEmptyInputValidator.))]
           (->> @(lsp-client/request! client [:textDocument/rename
                                              {:text-document {:uri (editor/editor->uri editor)}
                                               :position {:line line
                                                          :character character}
                                               :new-name new-name}])
                (editor/apply-workspace-edit project "LSP Rename" true))))))))
