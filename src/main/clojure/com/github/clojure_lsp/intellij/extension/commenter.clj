(ns com.github.clojure-lsp.intellij.extension.commenter
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.Commenter
   :implements [com.intellij.lang.Commenter])
  (:import
   [com.intellij.util.containers ContainerUtil]))

(set! *warn-on-reflection* true)

(defn -getLineCommentPrefix [_] ";;")
(defn -getBlockCommentPrefix [_] nil)
(defn -getBlockCommentSuffix [_] nil)
(defn -getCommentedBlockCommentPrefix [_] nil)
(defn -getCommentedBlockCommentSuffix [_] nil)
(defn -getLineCommentPrefixes [this]
  (ContainerUtil/createMaybeSingletonList (-getLineCommentPrefix this)))
