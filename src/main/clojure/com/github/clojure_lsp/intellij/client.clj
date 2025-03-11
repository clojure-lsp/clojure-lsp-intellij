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
    TextDocumentIdentifier
    WorkspaceSymbolParams]
   [org.eclipse.lsp4j.jsonrpc.messages Either]))

(set! *warn-on-reflection* true)

(defn project->ls-server-item ^LanguageServerItem
  [^Project project]
  (when-let [manager (LanguageServerManager/getInstance project)]
    @(.getLanguageServer manager "clojure-lsp")))

(defn ^:private project->ls-server ^ClojureLanguageServer [project]
  (when-let [item (project->ls-server-item project)]
    (.getServer item)))

(defn server-status [^Project project]
  (when-let [manager (LanguageServerManager/getInstance project)]
    (keyword (.toString (.getServerStatus manager "clojure-lsp")))))

(defn server-info [^Project project]
  (when (identical? :started (lsp-client/server-status project))
    (when-let [server (project->ls-server project)]
      (some->> (.serverInfo ^ClojureLanguageServer server)
               deref
               (into {})
               walk/keywordize-keys))))

(defn dependency-contents [^String uri ^Project project]
  (when-let [server (project->ls-server project)]
    (some->> (.dependencyContents ^ClojureLanguageServer server {"uri" uri})
             deref)))

(defn references [^String uri line character ^Project project]
  (when-let [server (project->ls-server project)]
    (some-> (.getTextDocumentService ^ClojureLanguageServer server)
            (.references (ReferenceParams. (TextDocumentIdentifier. uri)
                                           (Position. line character)
                                           (ReferenceContext. false)))
            deref)))

(defn symbols [^String query ^Project project]
  (when-let [server (project->ls-server project)]
    (some-> (.getWorkspaceService ^ClojureLanguageServer server)
            (.symbol (WorkspaceSymbolParams. query))
            ^Either deref
            .get)))

(defn execute-command [^String name ^String text ^List args ^Project project]
  (try
    (-> (CommandExecutor/executeCommand
         (doto (LSPCommandContext. (Command. text name args) project)
           (.setPreferredLanguageServerId "clojure-lsp")))
        (.response)
        deref)
    (catch Exception e
      (logger/error (format "Error applying command '%s' with args '%s' for text '%s'" name args text)
                    (with-out-str (.printStackTrace e))))))
