(ns com.github.clojure-lsp.intellij.shebang-test
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

(deftest shebang-comment-test
  "`#!` starts a line comment, same as `;`, so a shebang line at the top of
   a script is valid Clojure and must not be parsed as a dispatch form.
   Regression test for https://github.com/clojure-lsp/clojure-lsp-intellij/issues/71,
   where `#!/usr/bin/env bb` was reported as `<form> expected, got '/usr/bin/env'`."
  ;; Does not tear the fixture down: none of the other tests in this suite
  ;; do either, and LightIdeaTestFixtureImpl.tearDown() runs a JVM-wide
  ;; "all editors released" check that trips over editors other, unrelated
  ;; tests (e.g. slurp-action-test) leave open when they run earlier in the
  ;; same test task.
  (let [fixture (clj4intellij.test/setup "shebang-project")]
    (testing "shebang line followed by code (issue #71 repro)"
      (is (parses-cleanly? fixture "#!/usr/bin/env bb\n\n(require '[babashka.http-client :as http])")))
    (testing "bare shebang line with nothing else"
      (is (parses-cleanly? fixture "#!/usr/bin/env bb")))
    (testing "shebang comment is still a comment, not a form on its own"
      (is (parses-cleanly? fixture "#!shebang\n(+ 1 2)")))
    (testing "ordinary code is unaffected"
      (is (parses-cleanly? fixture "(defn foo [x] (+ x 1))")))
    (testing "regular line comments still work"
      (is (parses-cleanly? fixture "; a comment\n(+ 1 2)")))
    (testing "#_ discard still works"
      (is (parses-cleanly? fixture "#_(ignored) (+ 1 2)")))))
