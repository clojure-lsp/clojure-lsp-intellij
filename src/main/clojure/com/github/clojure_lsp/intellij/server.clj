(ns com.github.clojure-lsp.intellij.server
  (:import
   [com.intellij.openapi.project Project]
   [com.redhat.devtools.lsp4ij LanguageServerManager]))

(set! *warn-on-reflection* true)

(defn start-server! [^Project project]
  (.start (LanguageServerManager/getInstance project) "clojure-lsp"))

(defn shutdown! [^Project project]
  (.stop (LanguageServerManager/getInstance project) "clojure-lsp"))
