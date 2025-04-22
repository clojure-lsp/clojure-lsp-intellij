(ns com.github.clojure-lsp.intellij.test-utils
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.server :as server]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [com.github.ericdallo.clj4intellij.test :as clj4intellij.test])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]
   [com.intellij.ide DataManager]
   [com.intellij.openapi.actionSystem ActionManager]
   [com.intellij.openapi.components ServiceManager]
   [com.intellij.testFramework.fixtures CodeInsightTestFixture]))

(set! *warn-on-reflection* true)

(defn get-editor-text
  "Returns the text content of the editor's document."
  [^CodeInsightTestFixture fixture]
  (-> fixture .getEditor .getDocument .getText))

(defn open-file-in-editor
  "Opens a file in the editor and returns the editor instance."
  [^CodeInsightTestFixture fixture file]
  (let [project (.getProject fixture)]
    (app-manager/write-command-action
     project
     (fn [] (.openFileInEditor fixture file)))
    (.getEditor fixture)))

(defn run-editor-action
  "Runs an editor action with the given ID for the specified project."
  [action-id project]
  (let [action (.getAction (ActionManager/getInstance) action-id)
        context (.getDataContext (DataManager/getInstance))]
    (app-manager/write-command-action
     project
     (fn []
       (.actionPerformed
        action
        (com.intellij.openapi.actionSystem.AnActionEvent/createFromDataContext action-id nil context))))))

(defn wait-lsp-start
  "Dispatches all events until the LSP server is started or the timeout is reached."
  [{:keys [project millis timeout]
    :or {millis 1000
         timeout 10000}}]
  (let [start-time (System/currentTimeMillis)]
    (loop []
      (let [current-time (System/currentTimeMillis)
            elapsed-time (- current-time start-time)
            status (lsp-client/server-status project)]
        (cond
          (>= elapsed-time timeout)
          (throw (ex-info "LSP server failed to start within timeout"
                          {:elapsed-time elapsed-time
                           :final-status status}))

          (= status :started)
          true

          :else
          (do
            (clj4intellij.test/dispatch-all)
            (Thread/sleep millis)
            (recur)))))))

(defn teardown-test-project
  "Shuts down all resources for the given project."
  [project]
  (server/shutdown! project))

(defn setup-test-project
  "Sets up a test project with the given name and optional deps.edn content.
   Returns a map with :fixture, :project, and :deps-file."
  ([project-name]
   (setup-test-project project-name "{}"))
  ([project-name deps-content]
   (let [fixtures (clj4intellij.test/setup project-name)
         deps-file (.createFile fixtures "deps.edn" deps-content)
         _ (.setTestDataPath fixtures "testdata")
         project (.getProject fixtures)]
     {:fixtures fixtures
      :project project
      :deps-file deps-file})))

(defn setup-lsp-server
  "Sets up and waits for the LSP server to be ready."
  [project]
  (let [my-settings ^SettingsState (ServiceManager/getService SettingsState)]
    (.loadState my-settings my-settings)
    (clj4intellij.test/dispatch-all)
    (wait-lsp-start {:project project})))
