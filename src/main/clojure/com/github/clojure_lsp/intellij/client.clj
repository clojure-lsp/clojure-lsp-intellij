(ns com.github.clojure-lsp.intellij.client
  (:require
   [lsp4clj.protocols.endpoint :as protocols.endpoint])
  (:import
   [com.intellij.openapi.project Project]
   [com.redhat.devtools.lsp4ij LanguageServerManager]))

(set! *warn-on-reflection* true)

(defmulti show-message (fn [_context args] args))
(defmulti show-document (fn [_context args] args))
(defmulti show-message-request identity)
(defmulti progress (fn [_context {:keys [token]}] token))
(defmulti workspace-apply-edit (fn [_context {:keys [label]}] label))

(defn request! [client [method body]]
  (protocols.endpoint/send-request client (subs (str method) 1) body))

(defn notify! [client [method body]]
  (protocols.endpoint/send-notification client (subs (str method) 1) body))

(defn connected-server [^Project project]
  (when-let [manager (LanguageServerManager/getInstance project)]
    @(.getLanguageServer manager "clojure-lsp")))

(defn server-status [^Project project]
  (when-let [manager (LanguageServerManager/getInstance project)]
    (keyword (.toString (.getServerStatus manager "clojure-lsp")))))
