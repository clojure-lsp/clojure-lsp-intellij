(ns com.github.clojure-lsp.intellij.action.implementations
  (:gen-class
   :name com.github.clojure_lsp.intellij.action.ImplementationsAction
   :extends com.intellij.openapi.project.DumbAwareAction)
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.psi :as psi])
  (:import
   [com.intellij.codeInsight.hint HintManager]
   [com.intellij.codeInsight.navigation NavigationUtil]
   [com.intellij.find.findUsages FindUsagesOptions]
   [com.intellij.openapi.actionSystem CommonDataKeys]
   [com.intellij.openapi.actionSystem AnActionEvent]
   [com.intellij.openapi.editor Editor]
   [com.intellij.openapi.project Project]
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
  (when-let [client (lsp-client/connected-client (.getProject editor))]
    (let [implementations @(lsp-client/request! client [:textDocument/implementation
                                                        {:text-document {:uri (editor/editor->uri editor)}
                                                         :position {:line line
                                                                    :character character}}])
          project ^Project (.getProject editor)]
      (if (seq implementations)
        (if (= 1 (count implementations))
          (NavigationUtil/activateFileWithPsiElement (psi/element->psi-element (first implementations) project) true)
          (let [elements (->> implementations
                              (mapv #(psi/element->psi-element % project)))
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
                           (.setOpenInNewTab false)))))
        (.showErrorHint (HintManager/getInstance)
                        editor
                        "No implementations found")))))

(defn -actionPerformed [_ ^AnActionEvent event]
  (when-let [editor ^Editor (.getData event CommonDataKeys/EDITOR)]
    (let [[line character] (editor/editor->cursor-position editor)]
      (show-implementations editor line character))))
