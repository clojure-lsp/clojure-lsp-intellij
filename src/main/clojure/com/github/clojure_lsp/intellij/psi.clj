(ns com.github.clojure-lsp.intellij.psi
  (:require
   [clojure.string :as string])
  (:import
   [com.github.clojure_lsp.intellij ClojureLanguage]
   [com.github.clojure_lsp.intellij Icons]
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
  [^String name ^Project project ^PsiFile file
   ^Integer start-offset ^Integer end-offset
   ^Integer start-line]
  (let [manager (PsiManager/getInstance project)]
    (reify
      PsiNameIdentifierOwner
      (getName [_] (str (.getPresentableName (.getVirtualFile file)) ":" start-line))
      (setName [_ _])
      (getNameIdentifier [this] this)
      (getIdentifyingElement [this] this)
      (getProject [_] project)
      (getLanguage [_] ClojureLanguage/INSTANCE)
      (getManager [_] manager)
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
      (textToCharArray [_] (char-array name))
      (getNavigationElement [this] this)
      (getOriginalElement [this] this)
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
      (toString [_] (str name ":" start-offset ":" end-offset))
      (getContainingFile [_] file)

      (getUserData [_ _]
        nil)
      (putUserData [_ _ _])
      (getCopyableUserData [_ _])
      (putCopyableUserData [_ _ _])

      (getIcon [_ _] Icons/CLOJURE))))
