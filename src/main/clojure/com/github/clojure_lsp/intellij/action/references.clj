(ns com.github.clojure-lsp.intellij.action.references
  (:gen-class
   :name com.github.clojure_lsp.intellij.action.ReferencesAction
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

(defn get-references [^Editor editor line character]
  (let [project ^Project (.getProject editor)]
    (when-let [client (lsp-client/connected-client project)]
      (->> (lsp-client/request! client [:textDocument/references
                                        {:text-document {:uri (editor/editor->uri editor)}
                                         :position {:line line
                                                    :character character}}])
           deref
           (mapv #(psi/element->psi-element % project))))))

(defn show-references [^Editor editor line character]
  (when-let [references (get-references editor line character)]
    (if (seq references)
      (if (= 1 (count references))
        (NavigationUtil/activateFileWithPsiElement (first references) true)
        (let [project ^Project (.getProject editor)
              usages (mapv (fn [^PsiElement element]
                             (UsageInfo2UsageAdapter.
                              (UsageInfo. element false))) references)
              options (FindUsagesOptions. project)]
          (.showUsages (UsageViewManager/getInstance project)
                       (into-array UsageTarget [])
                       (into-array Usage usages)
                       (doto (UsageViewPresentation.)
                         (.setScopeText (.getDisplayName (.searchScope options)))
                         (.setSearchString (.generateUsagesString options))
                         (.setTabText (str (count references) " references"))
                         (.setTabName "references")
                         (.setShowCancelButton true)
                         (.setOpenInNewTab false)))))
      (.showErrorHint (HintManager/getInstance)
                      editor
                      "No references found"))))

(defn -actionPerformed [_ ^AnActionEvent event]
  (when-let [editor ^Editor (.getData event CommonDataKeys/EDITOR)]
    (let [[line character] (editor/editor->cursor-position editor)]
      (show-references editor line character))))
