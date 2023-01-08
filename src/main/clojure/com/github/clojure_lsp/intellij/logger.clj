(ns com.github.clojure-lsp.intellij.logger
  (:import
   (com.github.clojure_lsp.intellij WithLoader)
   (com.intellij.openapi.diagnostic Logger)))

(set! *warn-on-reflection* true)

(defonce ^Logger logger (Logger/getInstance WithLoader))

(defn info [& messages]
  (.info logger (str "[CLOJURE-LSP] " (apply format messages))))
