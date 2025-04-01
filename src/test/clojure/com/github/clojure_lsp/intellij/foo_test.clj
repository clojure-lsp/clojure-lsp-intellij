(ns com.github.clojure-lsp.intellij.foo-test
  (:require
   [clojure.test :refer [deftest is]]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [com.github.ericdallo.clj4intellij.test :as clj4intellij.test])
  (:import
   [com.intellij.openapi.wm WindowManager]
   [com.intellij.openapi.actionSystem ActionManager]
   [com.intellij.ide DataManager]))

(set! *warn-on-reflection* true)


(defn get-status-bar-widget [project widget-id]
  (let [status-bar (.. (WindowManager/getInstance) (getStatusBar project))]
    (.getWidget status-bar widget-id)))


(defn run-editor-action [action-id project]
  (let [action (.getAction (ActionManager/getInstance) action-id)
        context (.getDataContext (DataManager/getInstance))]


    (println "Running action:" action-id)
    (println "Action:" action)
    (app-manager/write-command-action
     project
     (fn []
       (.actionPerformed
        action
        (com.intellij.openapi.actionSystem.AnActionEvent/createFromDataContext action-id nil context))))))


(deftest foo-test
  (let [project-name "clojure.core"
        fixture (clj4intellij.test/setup project-name)
        deps-file (.createFile fixture "deps.edn" "{}")
        _ (.setTestDataPath fixture "testdata")
        clj-file (.copyFileToProject fixture "foo.clj")
        project (.getProject fixture)]
    (is (= project-name (.getName project)))
    (is deps-file)

    (app-manager/write-command-action
     project
     (fn [] (.openFileInEditor fixture clj-file)))



    (clj4intellij.test/dispatch-all)
    (println "OLAAAA >> ")
    (run-editor-action "ClojureLSP.ForwardSlurp" project)

    @(app-manager/invoke-later!
      {:invoke-fn (fn []
                    (let [widget (get-status-bar-widget project "ClojureLSPStatusBar")]
                      (println "Widget:" widget)
                      (is (some? widget))))})

    (.checkResultByFile fixture "foo_expected.clj")

    (is false)))


