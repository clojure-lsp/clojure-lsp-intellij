(ns com.github.clojure-lsp.intellij.client
  (:import
   [com.intellij.openapi.project Project]
   [com.redhat.devtools.lsp4ij LanguageServerManager]))

(set! *warn-on-reflection* true)

(defn server-status [^Project project]
  (when-let [manager (LanguageServerManager/getInstance project)]
    (keyword (.toString (.getServerStatus manager "clojure-lsp")))))
