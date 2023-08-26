(ns com.github.clojure-lsp.intellij.extension.rename-processor
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.RenameProcessor
   :extends com.intellij.refactoring.rename.RenamePsiElementProcessor)
  (:import
   [com.intellij.psi PsiElement]
   [com.github.clojure_lsp.intellij.language.psi CElement])
  (:require
    [com.github.clojure-lsp.intellij.logger :as logger]))

(set! *warn-on-reflection* true)

(defn -canProcessElement [_ ^PsiElement element]
  (logger/info "-------> %s" element)
  (instance? CElement element))

(defn -prepareRenaming [_ element new-name all-renames]
  )

(defn -findCollisions [_ element new-name all-renames result]
  )

(defn -isToSearchInComments [_ element]
  false)

(defn -setToSearchInComments [_ element enabled])
