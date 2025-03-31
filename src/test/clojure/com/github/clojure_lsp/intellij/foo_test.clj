(ns com.github.clojure-lsp.intellij.foo-test
  (:require
   [clojure.test :refer [deftest is testing]]))

(set! *warn-on-reflection* true)

(deftest foo
  (testing "foo"
    (is false)))
