(ns com.github.clojure-lsp.intellij.extension.dictionary
  (:require
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]])
  (:import
   [com.intellij.spellchecker BundledDictionaryProvider]))

(def-extension ClojureBundledDictionaryProvider []
  BundledDictionaryProvider
  (getBundledDictionaries [_]
    (into-array ["clojure.dic"])))
