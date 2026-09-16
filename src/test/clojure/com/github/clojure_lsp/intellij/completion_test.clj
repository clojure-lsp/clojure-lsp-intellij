(ns com.github.clojure-lsp.intellij.completion-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.test-utils :as test-utils]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [com.github.ericdallo.clj4intellij.test :as clj4intellij.test])
  (:import
   [com.intellij.codeInsight.lookup LookupElement LookupElementPresentation LookupElementRenderer LookupManager]
   [com.intellij.codeInsight.lookup.impl LookupImpl]
   [com.intellij.openapi.editor Editor]
   [com.intellij.testFramework.fixtures CodeInsightTestFixture]
   [com.redhat.devtools.lsp4ij.client.features LSPCompletionProposal]))

(set! *warn-on-reflection* true)

(defn ^:private proposal
  "LSP4IJ wraps its proposals in PrioritizedLookupElement decorators."
  ^LSPCompletionProposal [^LookupElement element]
  (let [object (.getObject element)]
    (when (instance? LSPCompletionProposal object)
      object)))

(defn ^:private complete-basic
  "Invokes basic completion until clojure-lsp returns items for the opened file,
   which may take a while since the file is analyzed asynchronously after didOpen."
  [^CodeInsightTestFixture fixture]
  (loop [attempt 0]
    (let [elements (->> (.completeBasic fixture)
                        (filter proposal)
                        vec)]
      (cond
        (seq elements) elements
        (>= attempt 20) (throw (ex-info "No LSP completion items returned" {:attempts attempt}))
        :else (do (clj4intellij.test/dispatch-all)
                  (Thread/sleep 500)
                  (recur (inc attempt)))))))

(defn ^:private select-item! [^LookupImpl lookup ^LookupElement element]
  @(app-manager/invoke-later! {:invoke-fn (fn [] (.setCurrentItem lookup element))})
  (clj4intellij.test/dispatch-all))

(deftest completion-test
  "Completion items must be our LSPCompletionProposal subclass which sends
   completionItem/resolve from the expensive renderer (background thread)
   instead of blocking the EDT inside getExpensiveRenderer."
  (let [project-name "clojure-completion-project"
        {:keys [fixtures project]} (test-utils/setup-test-project project-name)
        clj-file (.copyFileToProject ^CodeInsightTestFixture fixtures "completion.clj")
        ^Editor editor (test-utils/open-file-in-editor fixtures clj-file)]
    (test-utils/setup-lsp-server project)
    (editor/move-caret-to-position editor 6 7)
    (let [elements (complete-basic fixtures)
          labels (set (map #(.getLookupString ^LookupElement %) elements))
          ^LookupImpl lookup (LookupManager/getActiveLookup editor)
          current (some-> (.getCurrentItem lookup) proposal)]
      (testing "clojure-lsp items are created by our completion feature"
        (is (contains? labels "my-function-a"))
        (is (contains? labels "my-function-b"))
        (is (every? #(not= LSPCompletionProposal (class (proposal %))) elements)))
      (testing "the expensive renderer resolves the selected item off the EDT"
        ;; IntelliJ already scheduled the rendering of the item selected during
        ;; completion, so we pick another one and select it ourselves.
        (let [^LookupElement element (or (first (remove #(identical? current (proposal %)) elements))
                                         (first elements))
              proposal (proposal element)
              item (.getItem proposal)]
          (is (nil? (.getDetail item)))
          (is (.needToResolveCompletionDetail proposal))
          (is (nil? (.getExpensiveRenderer proposal)) "non selected items are not resolved")
          (select-item! lookup element)
          (let [renderer (.getExpensiveRenderer proposal)
                presentation (LookupElementPresentation.)]
            (is (instance? LookupElementRenderer renderer))
            (let [rendered (future (.renderElement ^LookupElementRenderer renderer proposal presentation))]
              (is (nil? (deref rendered 30000 ::timeout)))
              (is (some? (.getDetail item)))
              (is (not (.needToResolveCompletionDetail proposal)))
              (is (= (.getLookupString proposal) (.getItemText presentation)))))
          (is (some? (.computeDocumentation proposal))))))
    (test-utils/teardown-test-project project)))
