(ns com.github.clojure-lsp.intellij.server
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.workspace-edit])
  (:import
   [com.intellij.openapi.project Project]
   [com.redhat.devtools.lsp4ij LanguageServerItem LanguageServerManager]))

(set! *warn-on-reflection* true)

(def ^:private client-capabilities
  {:text-document {:hover {:content-format ["markdown"]}
                   :implementation {}}
   :workspace {:workspace-edit {:document-changes true
                                :resource-operations ["create" "rename"]}
               :file-operations {:will-rename true}}
   :window {:show-document true}})

(defn start-server! [^Project project]
  (when-let [item ^LanguageServerItem @(db/get-in project [:server])]
    (when-let [server ^LanguageServerManager (.getServer item)]
      (.start server "clojure-lsp"))))

(defn shutdown! [^Project project]
  (when-let [item ^LanguageServerItem @(db/get-in project [:server])]
    (when-let [server ^LanguageServerManager (.getServer item)]
      (.stop server "clojure-lsp"))))
