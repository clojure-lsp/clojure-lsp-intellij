(ns com.github.clojure-lsp.intellij.action.references
  (:gen-class
   :name com.github.clojure_lsp.intellij.action.ReferencesAction
   :extends com.intellij.openapi.project.DumbAwareAction)
  (:require
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.lsp-client :as lsp-client]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.github.clojure_lsp.intellij ClojureLanguage]
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.codeInsight.hint HintManager]
   [com.intellij.find.findUsages FindUsagesOptions]
   [com.intellij.navigation NavigationItem]
   [com.intellij.navigation ItemPresentation]
   [com.intellij.openapi.actionSystem CommonDataKeys]
   [com.intellij.openapi.actionSystem AnActionEvent]
   [com.intellij.openapi.editor Document Editor]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.util TextRange]
   [com.intellij.psi
    PsiElement
    PsiElementVisitor
    PsiFile
    PsiManager
    PsiNameIdentifierOwner
    PsiReference]
   [com.intellij.usages
    Usage
    UsageInfo2UsageAdapter
    UsageTarget
    UsageViewManager
    UsageViewPresentation]
   [com.intellij.usageView UsageInfo]))

(set! *warn-on-reflection* true)

(defn ->LSPPsiElement [^String name ^Project project ^PsiFile file ^Integer start ^Integer end]
  (let [manager (PsiManager/getInstance project)]
    (reify
      PsiNameIdentifierOwner
      (getNameIdentifier [this] this)
      (getProject [_] project)
      (getTextOffset [_] start)
      (getLanguage [_] (ClojureLanguage/INSTANCE))
      (getManager [_] manager)
      (getChildren [_] (into-array PsiElement []))
      (getParent [this] (.getContainingFile this))
      (getFirstChild [_])
      (getLastChild [_])
      (getNextSibling [_])
      (getPrevSibling [_])
      (getTextRange [_] (TextRange. start end))
      (getStartOffsetInParent [_] start)
      (getTextLength [_] (- end start))
      (findElementAt [_ _])
      (findReferenceAt [_ _])
      (textToCharArray [_] (char-array name))
      (getNavigationElement [this] this)
      (getOriginalElement [_])
      (^boolean textMatches [this ^CharSequence text]
        (= (.getText this) text))
      (^boolean textMatches [this ^PsiElement element]
        (.equals (.getText this) (.getText element)))
      (^boolean textContains [this ^char c]
        (string/includes? (.getText this) (str c)))
      (getText [_] name)
      (^void accept [this ^PsiElementVisitor visitor]
        (.visitElement visitor this))
      (acceptChildren [_ _])
      (copy [_])
      (add [_ _])
      (addBefore [_ _ _])
      (addAfter [_ _ _])
      (checkAdd [_ _])
      (addRange [_ _ _])
      (addRangeBefore [_ _ _ _])
      (addRangeAfter [_ _ _ _])
      (delete [_])
      (checkDelete [_])
      (deleteChildRange [_ _ _])
      (replace [_ _])
      (isValid [_] true)
      (isWritable [_] true)
      (getReference [_])
      (getReferences [_] (into-array PsiReference []))
      (processDeclarations [_ _ _ _ _] false)
      (getContext [_])
      (isPhysical [_] true)
      (getResolveScope [this] (.getResolveScope (.getContainingFile this)))
      (getUseScope [this] (.getResolveScope (.getContainingFile this)))
      (getNode [_])
      (isEquivalentTo [this other] (= this other))
      (toString [_] (str name ":" start ":" end))
      (getContainingFile [_] file)

      (getUserData [_ _]
        nil)
      (putUserData [_ _ _])

      (getIcon [_ _] Icons/ClojureFile)

      NavigationItem
      (getName [_] name)
      (getPresentation [this]
        (proxy+ [] ItemPresentation
                (getPresentableText [_] (.getName this))
                (getLocationString [_] (.getName (.getContainingFile this)))
                (getIcon [_ _])))
      (canNavigate [_] true)
      (canNavigateToSource [_] true)
      (navigate [_ _]
        ;; TODO
        ))))

(defn -actionPerformed [_ ^AnActionEvent event]
  (when-let [client (:client @db/db*)]
    (when-let [editor ^Editor (.getData event CommonDataKeys/EDITOR)]
      (let [[line character] (editor/editor->cursor-position editor)
            references @(lsp-client/request! client [:textDocument/references
                                                     {:text-document {:uri (editor/editor->uri editor)}
                                                      :position {:line line
                                                                 :character character}}])]
        (if (seq references)
          (let [project ^Project (.getProject editor)
                document ^Document (.getDocument editor)
                elements (->> references
                              (mapv (fn [{:keys [uri range]}]
                                      (let [start ^int (editor/position->point (:start range) document)
                                            end ^int (editor/position->point (:end range) document)
                                            name (.getText document (TextRange. start end))
                                            file (editor/uri->psi-file uri project)]
                                        (->LSPPsiElement name project file start end)))))
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
                           (.setTabText (str (count references) " references"))
                           (.setTabName "references")
                           (.setShowCancelButton true)
                           (.setOpenInNewTab false))))
          (.showErrorHint (HintManager/getInstance)
                          editor
                          "No references found"))))))
