(ns com.github.clojure-lsp.intellij.logger
  (:import
   (com.github.ericdallo.clj4intellij ClojureClassLoader)
   (com.intellij.openapi.diagnostic Logger)))

(set! *warn-on-reflection* true)

(defonce ^Logger logger (Logger/getInstance ClojureClassLoader))

(defn ^:private build-msg ^String [messages]
  (str "[CLOJURE-LSP] " (apply format (map str messages))))

(defn info [& messages]
  (.info logger (build-msg messages)))

(defn warn [& messages]
  (.warn logger (build-msg messages)))

(defn error [& messages]
  (.error logger (build-msg messages)))
