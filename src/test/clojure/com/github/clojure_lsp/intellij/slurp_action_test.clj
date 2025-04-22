(ns com.github.clojure-lsp.intellij.slurp-action-test
  (:require
   [clojure.test :refer [deftest is]]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.test-utils :as test-utils]
   [com.github.ericdallo.clj4intellij.test :as clj4intellij.test])
  (:import
   [com.intellij.openapi.project Project]
   [com.intellij.testFramework.fixtures CodeInsightTestFixture]))

(set! *warn-on-reflection* true)

(deftest slurp-action-test
  "Tests the Forward Slurp editor action functionality in Clojure LSP.
   This test:
   1. Sets up a test project with a Clojure file
   2. Opens the file in the editor
   3. Sets up the LSP server
   4. Moves the caret to a specific position
   5. Executes the Forward Slurp action
   6. Verifies the resulting text matches the expected output
   
   The test ensures that the Forward Slurp action correctly modifies the code structure
   by moving the closing parenthesis forward."
  (let [project-name "clojure.core"
        {:keys [fixture project deps-file]} (test-utils/setup-test-project project-name)
        clj-file (.copyFileToProject ^CodeInsightTestFixture fixture "foo.clj")]
    (is (= project-name (.getName ^Project project)))
    (is deps-file)

    (let [editor (test-utils/open-file-in-editor fixture clj-file)]
      (test-utils/setup-lsp-server project)
      (editor/move-caret-to-position editor 2 8)
      (test-utils/run-editor-action "ClojureLSP.ForwardSlurp" project)
      (clj4intellij.test/dispatch-all)
      (println (test-utils/get-editor-text fixture))
      (.checkResultByFile ^CodeInsightTestFixture fixture "foo_expected.clj")
      (test-utils/teardown-test-project project))))
