(ns com.github.clojure-lsp.intellij.foo-test
  (:require
   [clojure.test :refer [deftest is]]
   [com.github.clojure-lsp.intellij.test-utils :as test-utils]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [com.github.ericdallo.clj4intellij.test :as clj4intellij.test]))

(set! *warn-on-reflection* true)

(deftest foo-test
  (let [project-name "clojure.core"
        {:keys [fixture project deps-file]} (test-utils/setup-test-project project-name)
        clj-file (.copyFileToProject fixture "foo.clj")]
    (is (= project-name (.getName project)))
    (is deps-file)

    (let [editor (test-utils/open-file-in-editor fixture clj-file)]
      (test-utils/setup-lsp-server project)
      (test-utils/move-caret-to-position editor 2 8)
      (test-utils/run-editor-action "ClojureLSP.ForwardSlurp" project)
      (clj4intellij.test/dispatch-all)
      (println (test-utils/get-editor-text fixture))
      (test-utils/check-result-by-file fixture "foo_expected.clj")
      (test-utils/teardown-test-project project))))
