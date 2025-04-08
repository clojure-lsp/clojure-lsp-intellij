(ns com.github.clojure-lsp.intellij.test-utils 
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.server :as server]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [com.github.ericdallo.clj4intellij.test :as clj4intellij.test]
   [com.github.clojure-lsp.intellij.db :as db])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]
   [com.intellij.ide DataManager]
   [com.intellij.openapi.actionSystem ActionManager]
   [com.intellij.openapi.components ServiceManager]
   [com.intellij.openapi.editor LogicalPosition]
   [com.intellij.openapi.wm WindowManager]))

(set! *warn-on-reflection* true)

(defn get-status-bar-widget [project widget-id]
  (let [status-bar (.. (WindowManager/getInstance) (getStatusBar project))]
    (.getWidget status-bar widget-id)))

(defn run-editor-action
  "Runs an editor action with the given ID for the specified project."
  [action-id project]
  (let [action (.getAction (ActionManager/getInstance) action-id)
        context (.getDataContext (DataManager/getInstance))]
    (println "Running action:" action-id)
    (app-manager/write-command-action
     project
     (fn []
       (.actionPerformed
        action
        (com.intellij.openapi.actionSystem.AnActionEvent/createFromDataContext action-id nil context))))))

(defn dispatch-all-until
  "Dispatches all events until the LSP server is started or the timeout is reached."
  [{:keys [project millis timeout]
    :or {millis 1000
         timeout 10000}}]
  (let [start-time (System/currentTimeMillis)]
    (loop []
      (let [current-time (System/currentTimeMillis)
            elapsed-time (- current-time start-time)
            _ (println "Elapsed time >> " elapsed-time)
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
   (let [fixture (clj4intellij.test/setup project-name)
         deps-file (.createFile fixture "deps.edn" deps-content)
         _ (.setTestDataPath fixture "testdata")
         project (.getProject fixture)]
     {:fixture fixture
      :project project
      :deps-file deps-file})))

(defn open-file-in-editor
  "Opens a file in the editor and returns the editor instance."
  [fixture file]
  (let [project (.getProject fixture)]
    (app-manager/write-command-action
     project
     (fn [] (.openFileInEditor fixture file)))
    (.getEditor fixture)))

(defn move-caret-to-position
  "Moves the caret to the specified logical position in the editor."
  [editor line column]
  (let [caret (.getCaretModel editor)
        new-position (LogicalPosition. line column)]
    @(app-manager/invoke-later!
      {:invoke-fn (fn [] (.moveToLogicalPosition caret new-position))})))

(defn get-editor-text
  "Returns the text content of the editor's document."
  [fixture]
  (-> fixture .getEditor .getDocument .getText))

(defn check-result-by-file
  "Checks if the current editor content matches the expected file."
  [fixture expected-file]
  (.checkResultByFile fixture expected-file))

(defn setup-lsp-server
  "Sets up and waits for the LSP server to be ready."
  [project]
  (let [my-settings (ServiceManager/getService SettingsState)]
    (.loadState my-settings my-settings)
    (clj4intellij.test/dispatch-all)
    (dispatch-all-until {:project project})
    (println "status LSP >> " (db/get-in project [:status]))))