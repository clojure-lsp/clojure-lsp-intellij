(ns com.github.clojure-lsp.intellij.action.definition
  (:gen-class
   :name com.github.clojure_lsp.intellij.action.DefinitionAction
   :extends com.intellij.openapi.project.DumbAwareAction)
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.lsp-client :as lsp-client]
   [com.github.clojure-lsp.intellij.psi :as psi])
  (:import
   [com.intellij.codeInsight.hint HintManager]
   [com.intellij.find.findUsages FindUsagesOptions]
   [com.intellij.openapi.actionSystem CommonDataKeys]
   [com.intellij.openapi.actionSystem AnActionEvent]
   [com.intellij.openapi.editor Document Editor]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.util TextRange]
   [com.intellij.psi PsiElement]
   [com.intellij.usages
    Usage
    UsageInfo2UsageAdapter
    UsageTarget
    UsageViewManager
    UsageViewPresentation]
   [com.intellij.usageView UsageInfo]))

(set! *warn-on-reflection* true)

(defn ^:private find-definition [^Editor editor line character]
  (when-let [client (:client @db/db*)]
    (let [definition @(lsp-client/request! client [:textDocument/definition
                                                   {:text-document {:uri (editor/editor->uri editor)}
                                                    :position {:line line
                                                               :character character}}])]
      (if definition
        (let [project ^Project (.getProject editor)
              document ^Document (.getDocument editor)
              {:keys [uri range]} definition
              start ^int (editor/position->point (:start range) document)
              end ^int (editor/position->point (:end range) document)
              name (.getText document (TextRange. start end))
              file (editor/uri->psi-file uri project)

              elements [(psi/->LSPPsiElement name project file start end)]
              usages (mapv (fn [^PsiElement element]
                             (UsageInfo2UsageAdapter.
                              (UsageInfo. element false))) elements)
              options (FindUsagesOptions. project)]
          (.showUsages (UsageViewManager/getInstance project)
                       (into-array UsageTarget [])
                       (into-array Usage usages)
                       (doto (UsageViewPresentation.)
                         (.setScopeText (.getDisplayName (.searchScope options)))
                         (.setSearchString (.generateUsagesString options))
                         (.setTabText (str (count definition) " definitions"))
                         (.setTabName "definition")
                         (.setShowCancelButton true)
                         (.setOpenInNewTab false))))
        (.showErrorHint (HintManager/getInstance)
                        editor
                        "No definition found")))))

(defn -actionPerformed [_ ^AnActionEvent event]
  (when-let [editor ^Editor (.getData event CommonDataKeys/EDITOR)]
    (let [[line character] (editor/editor->cursor-position editor)]
      (find-definition editor line character))))
