(ns com.github.clojure-lsp.intellij.action.implementations
  (:gen-class
   :name com.github.clojure_lsp.intellij.action.ImplementationsAction
   :extends com.intellij.openapi.project.DumbAwareAction)
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor]
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

(defn show-implementations [^Editor editor line character]
  (when-let [client (:client @db/db*)]
    (let [implementations @(lsp-client/request! client [:textDocument/implementation
                                                        {:text-document {:uri (editor/editor->uri editor)}
                                                         :position {:line line
                                                                    :character character}}])]
      (if (seq implementations)
        (let [project ^Project (.getProject editor)
              elements (->> implementations
                            (mapv (fn [{:keys [uri range]}]
                                    (let [document ^Document (.getDocument (editor/uri->editor uri project))
                                          start ^int (editor/document+position->offset (:start range) document)
                                          end ^int (editor/document+position->offset (:end range) document)
                                          name (.getText document (TextRange. start end))
                                          file (editor/uri->psi-file uri project)]
                                      (psi/->LSPPsiElement name project file start end)))))
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
                         (.setTabText (str (count implementations) " implementations"))
                         (.setTabName "implementations")
                         (.setShowCancelButton true)
                         (.setOpenInNewTab false))))
        (.showErrorHint (HintManager/getInstance)
                        editor
                        "No implementations found")))))

(defn -actionPerformed [_ ^AnActionEvent event]
  (when-let [editor ^Editor (.getData event CommonDataKeys/EDITOR)]
    (let [[line character] (editor/editor->cursor-position editor)]
      (show-implementations editor line character))))
