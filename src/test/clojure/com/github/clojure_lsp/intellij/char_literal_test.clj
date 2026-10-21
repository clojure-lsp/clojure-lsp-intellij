(ns com.github.clojure-lsp.intellij.char-literal-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.github.ericdallo.clj4intellij.test :as clj4intellij.test])
  (:import
   [com.intellij.psi.util PsiTreeUtil]
   [com.intellij.testFramework.fixtures CodeInsightTestFixture]))

(set! *warn-on-reflection* true)

(defn ^:private parses-cleanly?
  "Parses `text` as a standalone .clj file and returns true when the
   resulting PSI tree has no error elements."
  [^CodeInsightTestFixture fixture ^String text]
  (let [psi-file (.configureByText fixture "a.clj" text)]
    (not (PsiTreeUtil/hasErrorElements psi-file))))

(deftest char-literal-sequence-test
  "Back-to-back char literals are valid Clojure: each `\\x` is self-delimited
   by its own leading backslash, so no separator is required between them.
   Regression test for https://github.com/clojure-lsp/clojure-lsp-intellij/issues/54,
   where the lexer merged two adjacent char literals into one BAD_CHARACTER
   token, reported by the parser as `<form> expected, got '...'`."
  ;; Does not tear the fixture down: none of the other tests in this suite
  ;; do either, and LightIdeaTestFixtureImpl.tearDown() runs a JVM-wide
  ;; "all editors released" check that trips over editors other, unrelated
  ;; tests (e.g. slurp-action-test) leave open when they run earlier in the
  ;; same test task.
  (let [fixture (clj4intellij.test/setup "char-literal-project")]
    (testing "two bare char literals back to back (issue #54 repro)"
      (is (parses-cleanly? fixture "\\a\\b")))
    (testing "char literals back to back inside a vector (issue #54 repro)"
      (is (parses-cleanly? fixture "[\\!\\@]")))
    (testing "three char literals back to back"
      (is (parses-cleanly? fixture "\\a\\b\\c")))
    (testing "a single char literal still parses"
      (is (parses-cleanly? fixture "\\newline")))
    (testing "a char literal glued to a symbol is still flagged as invalid"
      (is (not (parses-cleanly? fixture "\\afoo"))))
    (testing "ordinary code is unaffected"
      (is (parses-cleanly? fixture "(defn foo [x] (+ x 1))")))))
