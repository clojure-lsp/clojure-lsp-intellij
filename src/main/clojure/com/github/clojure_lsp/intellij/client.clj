(ns com.github.clojure-lsp.intellij.client
  (:require
   [clojure.walk :as walk]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.ericdallo.clj4intellij.logger :as logger])
  (:import
   [com.github.clojure_lsp.intellij ClojureLanguageServer]
   [com.intellij.openapi.project Project]
   [com.redhat.devtools.lsp4ij LanguageServerItem LanguageServerManager]
   [com.redhat.devtools.lsp4ij.commands CommandExecutor LSPCommandContext]
   [java.util List]
   [org.eclipse.lsp4j
    Command
    Position
    ReferenceContext
    ReferenceParams
    TextDocumentIdentifier]))

(set! *warn-on-reflection* true)

(defn server-status [^Project project]
  (when-let [manager (LanguageServerManager/getInstance project)]
    (keyword (.toString (.getServerStatus manager "clojure-lsp")))))

(defn server-info [^Project project]
  (when (identical? :started (lsp-client/server-status project))
    (when-let [manager (LanguageServerManager/getInstance project)]
      (when-let [server (.getServer ^LanguageServerItem @(.getLanguageServer manager "clojure-lsp"))]
        (some->> (.serverInfo ^ClojureLanguageServer server)
                 deref
                 (into {})
                 walk/keywordize-keys)))))

(defn dependency-contents [^String uri ^Project project]
  (when-let [manager (LanguageServerManager/getInstance project)]
    (when-let [server (.getServer ^LanguageServerItem @(.getLanguageServer manager "clojure-lsp"))]
      (some->> (.dependencyContents ^ClojureLanguageServer server {"uri" uri})
               deref))))

(defn references [^String uri line character ^Project project]
  (when-let [manager (LanguageServerManager/getInstance project)]
    (when-let [server (.getServer ^LanguageServerItem @(.getLanguageServer manager "clojure-lsp"))]
      (some-> (.getTextDocumentService ^ClojureLanguageServer server)
              (.references (ReferenceParams. (TextDocumentIdentifier. uri)
                                             (Position. line character)
                                             (ReferenceContext. false)))
              deref))))

(defn execute-command [^String name ^String text ^List args ^Project project]
  (-> (CommandExecutor/executeCommand
        (doto (LSPCommandContext. (Command. text name args) project)
          (.setPreferredLanguageServerId "clojure-lsp")))
      (.response)
      deref))
