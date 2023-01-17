(ns com.github.clojure-lsp.intellij.extension.bundled-dictionary-provider
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.BundledDictionaryProvider
   :extends com.github.clojure_lsp.intellij.WithLoader
   :implements [com.intellij.spellchecker.BundledDictionaryProvider]))

(defn -getBundledDictionaries [_]
  (into-array ["clojure.dic"]))
