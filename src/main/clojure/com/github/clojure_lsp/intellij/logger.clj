(ns com.github.clojure-lsp.intellij.logger
  (:import
   (com.github.clojure_lsp.intellij WithLoader)
   (com.intellij.openapi.diagnostic Logger)))

(set! *warn-on-reflection* true)

(defonce ^Logger logger (Logger/getInstance WithLoader))

(defn info [^String message]
  (.info logger message))
