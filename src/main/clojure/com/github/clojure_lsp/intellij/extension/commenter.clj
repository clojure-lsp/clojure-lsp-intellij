(ns com.github.clojure-lsp.intellij.extension.commenter
  (:require
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]])
  (:import
   [com.intellij.lang Commenter]
   [com.intellij.util.containers ContainerUtil]))

(set! *warn-on-reflection* true)

(def ^:private comment-single-line ";; ")

(def-extension ClojureCommenter []
  Commenter
  (getLineCommentPrefix [_] comment-single-line)
  (getBlockCommentPrefix [_] nil)
  (getBlockCommentSuffix [_] nil)
  (getCommentedBlockCommentPrefix [_] nil)
  (getCommentedBlockCommentSuffix [_] nil)
  (getLineCommentPrefixes [_]
    (ContainerUtil/createMaybeSingletonList comment-single-line)))
