(ns com.github.clojure-lsp.intellij.foo-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.server :as server]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [com.github.ericdallo.clj4intellij.test :as clj4intellij.test])
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

       ;; Para configurações persistentes via ServiceManager
    (let [my-settings (ServiceManager/getService SettingsState)] ;; Substitua pela classe real
      (.setServerPath my-settings "/tmp/clojure-lsp") ;; Atualiza o caminho do servidor
      (.loadState my-settings my-settings));; Atualiza estado
    (println "LSP exists? >> ")
    (println (.exists (io/as-file "/tmp/clojure-lsp")))
    (server/start! project)

    #_(clj4intellij.test/dispatch-all)
    (println "status LSP >> ")
    (println (lsp-client/server-status project))
    (println (db/get-in project [:status]))
    (Thread/sleep 10000)
    #_(clj4intellij.test/dispatch-all-until
       {:cond-fn (fn [] (= (db/get-in project [:status]) :started))
        :millis 3000})
    (println "status LSP >> ")
    (println (lsp-client/server-status project))
    (println (db/get-in project [:status]))

    (let [editor (.getEditor fixture)
          document (.getDocument editor)
          offset (.getLineStartOffset document 2)
          caret (.getCaretModel editor)
          pos (.getLogicalPosition caret)
          new-position (LogicalPosition. 2 8)]
      (println "editor >> ")
      (println editor)
      (println caret)
      (println pos)
      (println (.getVisualPosition caret))
      (println (.getText document))
      @(app-manager/invoke-later!
        {:invoke-fn (fn []
                      #_(.moveToOffset caret (+ offset 9))
                      (.moveToLogicalPosition caret new-position))})
      (println (.getLogicalPosition caret))
      (println (.getVisualPosition caret)))
    (run-editor-action "ClojureLSP.ForwardSlurp" project)

    (clj4intellij.test/dispatch-all)
    (println (-> fixture .getEditor .getDocument .getText))

    @(app-manager/invoke-later!
      {:invoke-fn (fn []
                    (let [widget (get-status-bar-widget project "ClojureLSPStatusBar")]
                      (println "Widget:" widget)
                      (is (some? widget))))})

    (.checkResultByFile fixture "foo_expected.clj")

    (is false)))


