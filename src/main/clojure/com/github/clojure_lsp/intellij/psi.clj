(ns com.github.clojure-lsp.intellij.psi
  (:require
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [com.github.ericdallo.clj4intellij.util :as util])
  (:import
   [com.github.clojure_lsp.intellij ClojureLanguage]
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.navigation ItemPresentation NavigationItem PsiElementNavigationItem]
   [com.intellij.openapi.editor ScrollType]
   [com.intellij.openapi.fileEditor FileEditorManager]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.util TextRange]
   [com.intellij.psi
    PsiElement
    PsiElementVisitor
    PsiFile
    PsiManager
    PsiNameIdentifierOwner
    PsiReference]))

(set! *warn-on-reflection* true)

(defn ->LSPPsiElement
  [^String text ^Project project ^PsiFile file
   ^Integer start-offset ^Integer end-offset
   ^Integer start-line]
  (let [psi-manager (PsiManager/getInstance project)
        file-editor-manager (FileEditorManager/getInstance project)]
    (reify
      PsiNameIdentifierOwner
      (getName [_]
        (str (editor/filename->project-relative-filename
               (.getCanonicalPath (.getVirtualFile file))
               project)
             "  "
             start-line))
      (setName [_ _])
      (getNameIdentifier [this] this)
      (getIdentifyingElement [this] this)
      (getProject [_] project)
      (getLanguage [_] ClojureLanguage/INSTANCE)
      (getManager [_] psi-manager)
      (getChildren [_] (into-array PsiElement []))
      (getParent [this] (.getContainingFile this))
      (getFirstChild [_])
      (getLastChild [_])
      (getNextSibling [_])
      (getPrevSibling [_])
      (getTextRange [_] (TextRange. start-offset end-offset))
      (getTextOffset [_] start-offset)
      (getStartOffsetInParent [_] start-offset)
      (getTextLength [_] (- end-offset start-offset))
      (findElementAt [_ _])
      (findReferenceAt [_ _])
      (textToCharArray [_] (char-array text))
      (getNavigationElement [this] this)
      (getOriginalElement [this] this)
      (^boolean textMatches [this ^CharSequence text]
        (= (.getText this) text))
      (^boolean textMatches [this ^PsiElement element]
        (.equals (.getText this) (.getText element)))
      (^boolean textContains [this ^char c]
        (string/includes? (.getText this) (str c)))
      (getText [_] text)
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
      (toString [_] (str text ":" start-offset ":" end-offset))
      (getContainingFile [_] file)

      PsiElementNavigationItem
      (getTargetElement [this] this)

      NavigationItem
      (getPresentation [this]
        (reify ItemPresentation
          (getPresentableText [_] (.getName this))
          (getIcon [_ _] Icons/CLOJURE)))
      (canNavigate [_] true)
      (canNavigateToSource [_] true)
      (navigate [_ _]
        (let [editor (util/v-file->editor (.getVirtualFile file) project true)]
          (.openFile file-editor-manager (.getVirtualFile file) true)
          (.moveToOffset (.getCaretModel editor) start-offset)
          (.scrollToCaret (.getScrollingModel editor) ScrollType/CENTER)))

      (getUserData [_ _] nil)
      (putUserData [_ _ _])
      (getCopyableUserData [_ _])
      (putCopyableUserData [_ _ _])

      (getIcon [_ _] Icons/CLOJURE))))

(defn element->psi-element [{:keys [uri] {:keys [start end]} :range} project]
  (let [text (slurp uri)
        start-offset (editor/position->offset text (:line start) (:character start))
        end-offset (editor/position->offset text (:line end) (:character end))
        file (editor/uri->psi-file uri project)
        name (subs text start-offset end-offset)]
    (->LSPPsiElement name project file start-offset end-offset (:line start))))
