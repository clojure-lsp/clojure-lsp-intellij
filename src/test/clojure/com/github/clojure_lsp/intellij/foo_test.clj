(ns com.github.clojure-lsp.intellij.foo-test
  (:require
   [clojure.test :refer [deftest is]]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.server :as server]
   [com.github.clojure-lsp.intellij.test-utils :as test-utils]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [com.github.ericdallo.clj4intellij.test :as clj4intellij.test])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState] 
   [com.intellij.openapi.components ServiceManager]
   [com.intellij.openapi.editor LogicalPosition]))

(set! *warn-on-reflection* true)









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
      (.loadState my-settings my-settings));; Atualiza estado
    
    (clj4intellij.test/dispatch-all) 
    (test-utils/dispatch-all-until {:project project})
    (println "status LSP >> " (db/get-in project [:status])) 
    (let [editor (.getEditor fixture)
          document (.getDocument editor)
          offset (.getLineStartOffset document 2)
          caret (.getCaretModel editor)
          pos (.getLogicalPosition caret)
          new-position (LogicalPosition. 2 8)]
      (println (.getText document))
      @(app-manager/invoke-later!
        {:invoke-fn (fn []
                      #_(.moveToOffset caret (+ offset 9))
                      (.moveToLogicalPosition caret new-position))}))
    (test-utils/run-editor-action "ClojureLSP.ForwardSlurp" project)
    (clj4intellij.test/dispatch-all)
    (println (-> fixture .getEditor .getDocument .getText)) 
    (.checkResultByFile fixture "foo_expected.clj")
    (server/shutdown! project)))


