(ns com.github.clojure-lsp.intellij.qualified-name-provider-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.github.clojure-lsp.intellij.test-utils :as test-utils]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [com.github.ericdallo.clj4intellij.test :as clj4intellij.test])
  (:import
   [com.intellij.ide.actions QualifiedNameProviderUtil]
   [com.intellij.openapi.editor Document Editor]
   [com.intellij.psi PsiDocumentManager PsiFile]
   [com.intellij.testFramework.fixtures CodeInsightTestFixture]))

(set! *warn-on-reflection* true)

(defn ^:private qualified-name-at
  "Qualified name the Copy Reference action gets for the PSI element at line/column,
   retried while clojure-lsp analyzes the freshly opened file."
  [^CodeInsightTestFixture fixture line column]
  (let [project (.getProject fixture)
        ^Editor editor (.getEditor fixture)
        ^Document document (.getDocument editor)
        offset (+ (.getLineStartOffset document line) column)
        compute #(deref (app-manager/read-action!
                         {:run-fn (fn []
                                    (let [^PsiFile file (.getPsiFile (PsiDocumentManager/getInstance project) document)]
                                      (some-> (.findElementAt file offset)
                                              QualifiedNameProviderUtil/getQualifiedName)))}))]
    (loop [attempt 0]
      (or (compute)
          (when (< attempt 20)
            (clj4intellij.test/dispatch-all)
            (Thread/sleep 500)
            (recur (inc attempt)))))))

(deftest qualified-name-provider-test
  (let [project-name "clojure-qualified-name-project"
        {:keys [fixtures project]} (test-utils/setup-test-project project-name)
        clj-file (.copyFileToProject ^CodeInsightTestFixture fixtures "qualified_name.clj")]
    (test-utils/open-file-in-editor fixtures clj-file)
    (test-utils/setup-lsp-server project)
    (testing "a var defined in the current namespace"
      (is (= "bar.foo/aa" (qualified-name-at fixtures 6 8)))
      (is (= "bar.foo/aa" (qualified-name-at fixtures 4 5))))
    (testing "a var used through a namespace alias"
      (is (= "clojure.string/join" (qualified-name-at fixtures 8 6))))
    (testing "locals have no qualified name"
      (is (nil? (qualified-name-at fixtures 10 16))))
    (test-utils/teardown-test-project project)))
