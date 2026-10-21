(ns com.github.clojure-lsp.intellij.paredit-editor-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.github.clojure-lsp.intellij.settings :as settings]
   [com.github.clojure-lsp.intellij.test-utils :as test-utils])
  (:import
   [com.intellij.openapi.actionSystem IdeActions]
   [com.intellij.testFramework EdtTestUtil]
   [com.intellij.testFramework.fixtures CodeInsightTestFixture]
   [com.intellij.util ThrowableRunnable]))

(set! *warn-on-reflection* true)

(defn ^:private in-editor
  "Opens `before` (with a <caret> marker) in the editor, runs `action` with the
   fixture on the EDT and returns the resulting text with the caret marker."
  [^CodeInsightTestFixture fixture before action]
  (let [result (promise)]
    (EdtTestUtil/runInEdtAndWait
     (reify ThrowableRunnable
       (run [_]
         (.configureByText fixture "foo.clj" ^String before)
         (action fixture)
         (let [editor (.getEditor fixture)
               text (.getText (.getDocument editor))
               offset (.getOffset (.getCaretModel editor))]
           (deliver result (str (subs text 0 offset) "<caret>" (subs text offset)))))))
    @result))

(defn ^:private typing [^String s]
  (fn [^CodeInsightTestFixture fixture] (.type fixture s)))

(defn ^:private editor-action [^String id]
  (fn [^CodeInsightTestFixture fixture] (.performEditorAction fixture id)))

(deftest paredit-editor-test
  (let [{:keys [fixtures project]} (test-utils/setup-test-project "paredit-project")]
    (try
      (testing "strict mode off keeps the default typing"
        (settings/set-paredit-enabled! false)
        (is (= "(a )<caret>b)" (in-editor fixtures "(a <caret>b)" (typing ")")))))

      (settings/set-paredit-enabled! true)
      (testing "typing keeps brackets balanced"
        (is (= "(a b)<caret>" (in-editor fixtures "(a <caret>b)" (typing ")"))))
        (is (= "(a [<caret>])" (in-editor fixtures "(a <caret>)" (typing "["))))
        (is (= "(a \"x\\\"<caret>y\")" (in-editor fixtures "(a \"x<caret>y\")" (typing "\""))))
        (is (= "(a \"x\"<caret>)" (in-editor fixtures "(a \"x<caret>\")" (typing "\"")))))
      (testing "backspace and delete step over non-empty pairs"
        (is (= "(a (b<caret>))" (in-editor fixtures "(a (b)<caret>)" (editor-action IdeActions/ACTION_EDITOR_BACKSPACE))))
        (is (= "(a <caret>)" (in-editor fixtures "(a (<caret>))" (editor-action IdeActions/ACTION_EDITOR_BACKSPACE))))
        (is (= "(a (<caret>b))" (in-editor fixtures "(a <caret>(b))" (editor-action IdeActions/ACTION_EDITOR_DELETE))))
        (is (= "(a<caret>)" (in-editor fixtures "(ab<caret>)" (editor-action IdeActions/ACTION_EDITOR_BACKSPACE)))))
      (testing "removals that would break the balance are refused"
        (is (= "(a (b<caret> c))" (in-editor fixtures "(a <selection>(b</selection> c))" (typing "x"))))
        (is (= "(a ;<caret>\nb)" (in-editor fixtures "(a <caret>b)" (typing ";"))))
        (is (= "(a \\<caret> )" (in-editor fixtures "(a <caret>)" (typing "\\"))))
        (is (= "(a <caret>)" (in-editor fixtures "(a<caret>)" (typing " "))))
        (is (= "(a [<caret>b c])" (in-editor fixtures "(a <selection>b c</selection>)" (typing "["))))
        (is (= "(a\n <caret>(b\n c))" (in-editor fixtures "(a\n <caret>(b\n c))" (editor-action IdeActions/ACTION_EDITOR_DELETE_LINE))))
        (is (= "(a\n <caret>(b\n c))" (in-editor fixtures "(a\n <caret>(b\n c))" (editor-action IdeActions/ACTION_EDITOR_CUT))))
        (is (= "(a)\n<caret>(c)" (in-editor fixtures "(a)\n<caret>(b)\n(c)" (editor-action IdeActions/ACTION_EDITOR_DELETE_LINE)))))
      (testing "structural editing actions"
        (is (= "(a b <caret>c)" (in-editor fixtures "(a (b <caret>c))" (editor-action "ClojureLSP.SpliceSexp"))))
        (is (= "(a (<caret>b) c)" (in-editor fixtures "(a <caret>b c)" (editor-action "ClojureLSP.WrapRound"))))
        (is (= "(a b)<caret> (c)" (in-editor fixtures "(a b<caret> c)" (editor-action "ClojureLSP.SplitSexp"))))
        (is (= "(a (b c)<caret>)" (in-editor fixtures "(a (b <caret>c))" (editor-action "ClojureLSP.ForwardUp")))))
      (finally
        (settings/set-paredit-enabled! false)
        (test-utils/teardown-test-project project)))))
