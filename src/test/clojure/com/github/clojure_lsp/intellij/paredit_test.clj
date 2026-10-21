(ns com.github.clojure-lsp.intellij.paredit-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [com.github.clojure-lsp.intellij.paredit :as paredit]))

(defn ^:private run
  "Calls (f text offset & args) on `code`, where `|` marks the caret, and
   returns the resulting code with the new caret, or :default when the
   operation leaves the keystroke to the editor."
  [f code & args]
  (let [offset (str/index-of code "|")
        text (str (subs code 0 offset) (subs code (inc offset)))]
    (if-let [result (apply f text offset args)]
      (let [[new-text caret] (paredit/apply-edits text result)]
        (str (subs new-text 0 caret) "|" (subs new-text caret)))
      :default)))

(defn ^:private run-selection
  "Like `run` for operations on a selection marked with « and »."
  [f code & args]
  (let [start (str/index-of code "«")
        end (dec (str/index-of code "»"))
        text (str/replace code #"[«»]" "")]
    (if-let [result (apply f text start end args)]
      (let [[new-text caret] (paredit/apply-edits text result)]
        (str (subs new-text 0 caret) "|" (subs new-text caret)))
      :default)))

(deftest parse-test
  (testing "reader prefixes belong to the next form"
    (is (= [[:coll 0 4] [:coll 5 10] [:string 11 15] [:coll 16 20]]
           (map (juxt :type :start :end) (:children (paredit/parse "#{1} #?(a) #\"x\" '(b)"))))))
  (testing "char literals, strings and comments do not open forms"
    (is (= [:coll]
           (map :type (:children (paredit/parse "(\\( \")\" ; )\n)"))))))
  (testing "shallow parsing counts the same errors as a full parse"
    (doseq [text ["(a (b] c" "(a \"x" "a) (b" "[(])" "(\\( ;)\n" "{(a [b)} c"]]
      (is (= (#'paredit/error-count (paredit/parse text))
             (#'paredit/error-count (paredit/parse text -1 -1)))
          text))))

(deftest insert-pair-test
  (is (= "(a (|))" (run paredit/insert-pair "(a |)" \()))
  (is (= "[|]" (run paredit/insert-pair "|" \[)))
  (is (= "#{|}" (run paredit/insert-pair "#|" \{)))
  (testing "inside strings, comments and char literals the editor decides"
    (is (= :default (run paredit/insert-pair "\"a|\"" \()))
    (is (= :default (run paredit/insert-pair "; a|" \()))
    (is (= :default (run paredit/insert-pair "(\\|)" \()))))

(deftest insert-closer-test
  (is (= "(a b)|" (run paredit/insert-closer "(a |b)")))
  (is (= "(a [b c]|)" (run paredit/insert-closer "(a [b |c])")))
  (is (= "a|" (run paredit/insert-closer "a|")))
  (testing "an unclosed form can be closed by hand"
    (is (= :default (run paredit/insert-closer "(a |"))))
  (is (= :default (run paredit/insert-closer "\"a|\""))))

(deftest insert-quote-test
  (is (= "(\"|\")" (run paredit/insert-quote "(|)")))
  (is (= "\"ab\"|" (run paredit/insert-quote "\"ab|\"")))
  (is (= "\"a\\\"|b\"" (run paredit/insert-quote "\"a|b\"")))
  (is (= :default (run paredit/insert-quote "; a|")))
  (is (= :default (run paredit/insert-quote "\\|"))))

(deftest insert-semicolon-test
  (is (= "(a ;|\nb)" (run paredit/insert-semicolon "(a |b)")))
  (is (= "(a ;|\n)" (run paredit/insert-semicolon "(a |)")))
  (testing "a semicolon that comments out nothing structural is typed as is"
    (is (= :default (run paredit/insert-semicolon "(a)\n|b")))
    (is (= :default (run paredit/insert-semicolon "\"a|)\"")))))

(deftest insert-backslash-test
  (is (= "(a \\| )" (run paredit/insert-backslash "(a |)")))
  (is (= "(a \\| \")\")" (run paredit/insert-backslash "(a |\")\")")))
  (is (= :default (run paredit/insert-backslash "(a | b)")))
  (is (= :default (run paredit/insert-backslash "\"a|)\""))))

(deftest replace-selection-test
  (is (= :default (run-selection paredit/replace-selection "(a «b c»)")))
  (is (= "(a (b| c))" (run-selection paredit/replace-selection "(a «(b» c))")))
  (is (paredit/region-removable? "(a)\n(b\n c)" 0 4))
  (is (not (paredit/region-removable? "(a)\n(b\n c)" 4 7))))

(deftest backspace-test
  (testing "empty pairs are deleted"
    (is (= "(a |)" (run paredit/backspace "(a (|))")))
    (is (= "(a |)" (run paredit/backspace "(a #{|})")))
    (is (= "(a |)" (run paredit/backspace "(a \"|\")"))))
  (testing "non-empty pairs are stepped over, not deleted"
    (is (= "(a |(b))" (run paredit/backspace "(a (|b))")))
    (is (= "(a (b|))" (run paredit/backspace "(a (b)|)")))
    (is (= "(a \"b|\")" (run paredit/backspace "(a \"b\"|)")))
    (is (= "|\"b\"" (run paredit/backspace "\"|b\""))))
  (testing "escapes and char literals are deleted whole"
    (is (= "\"a|\"" (run paredit/backspace "\"a\\\"|\"")))
    (is (= "(a |)" (run paredit/backspace "(a \\(|)")))
    (is (= "(a |)" (run paredit/backspace "(a \\|()"))))
  (testing "ordinary chars use the editor default"
    (is (= :default (run paredit/backspace "(ab|)")))
    (is (= :default (run paredit/backspace "; (|"))))
  (testing "a stray closer can be removed"
    (is (= :default (run paredit/backspace "(a))|")))))

(deftest delete-forward-test
  (is (= "(a |)" (run paredit/delete-forward "(a |())")))
  (is (= "(a (|b))" (run paredit/delete-forward "(a |(b))")))
  (is (= "(a (b)|)" (run paredit/delete-forward "(a (b|))")))
  (is (= "(a #{|b})" (run paredit/delete-forward "(a |#{b})")))
  (is (= "\"ab\"|" (run paredit/delete-forward "\"ab|\"")))
  (is (= "\"a|\"" (run paredit/delete-forward "\"a|\\n\"")))
  (is (= "(|)" (run paredit/delete-forward "(|\\))")))
  (is (= :default (run paredit/delete-forward "(|ab)"))))

(deftest delete-selection-test
  (is (= "(a |)" (run-selection paredit/delete-selection "(a «(b c)»)")))
  (is (= "(a (| c))" (run-selection paredit/delete-selection "(a («b» c))")))
  (testing "unbalanced selections are kept"
    (is (= "(a (b| c))" (run-selection paredit/delete-selection "(a «(b» c))")))))

(deftest kill-test
  (is (= "(a |)" (run paredit/kill "(a |b c)")))
  (is (= "(a |\n b)" (run paredit/kill "(a |(x\n y)\n b)")))
  (is (= "(a | b)" (run paredit/kill "(a |\n b)")))
  (is (= "\"a|\"" (run paredit/kill "\"a|bc\"")))
  (is (= "; a|\n(b)" (run paredit/kill "; a| c\n(b)")))
  (is (= :default (run paredit/kill "(a|)")))
  (is (= "b c" (:killed (paredit/kill "(a b c)" 3)))))

(deftest wrap-test
  (is (= "(a (|b) c)" (run paredit/wrap "(a |b c)" 3 \()))
  (is (= "(a [|(b)])" (run paredit/wrap "(a |(b))" 3 \[)))
  (is (= "(a {|})" (run paredit/wrap "(a |)" 3 \{)))
  (is (= "([|a b])" (run-selection paredit/wrap "(«a b»)" \[)))
  (is (= :default (run-selection paredit/wrap "(«a (b» c))" \())))

(deftest splice-test
  (is (= "(a b |c)" (run paredit/splice "(a (b |c))")))
  (is (= "(a b |c)" (run paredit/splice "(a #{b |c})")))
  (is (= :default (run paredit/splice "a |b")))
  (is (= "(a |c)" (run paredit/splice-killing-backward "(a (b |c))")))
  (is (= "(a b |)" (run paredit/splice-killing-forward "(a (b |c))"))))

(deftest split-test
  (is (= "(a b)| (c)" (run paredit/split "(a b| c)")))
  (is (= "#{a}| #{b}" (run paredit/split "#{a |b}")))
  (is (= "(a)|\n (b)" (run paredit/split "(a |\n b)")))
  (is (= "\"ab\"| \"cd\"" (run paredit/split "\"ab|cd\"")))
  (is (= :default (run paredit/split "a |b"))))

(deftest join-test
  (is (= "(a| b)" (run paredit/join "(a) |(b)")))
  (is (= "[a| b]" (run paredit/join "[a]| [b]")))
  (is (= "\"ab|cd\"" (run paredit/join "\"ab\" |\"cd\"")))
  (is (= :default (run paredit/join "(a) |[b]")))
  (is (= :default (run paredit/join "{a 1} |#{b}"))))

(deftest navigation-test
  (is (= "(a |(b c))" (run paredit/backward-up "(a (b |c))")))
  (is (= "|(a (b c))" (run paredit/backward-up "(a |(b c))")))
  (is (= "(a (b c)|)" (run paredit/forward-up "(a (b |c))")))
  (is (= :default (run paredit/forward-up "a |b")))
  (is (= "(a (|b c))" (run paredit/forward-down "(a |(b c))")))
  (is (= "(|a)" (run paredit/forward-down "|(a)")))
  (is (= "(a (b c|) d)" (run paredit/backward-down "(a (b c) |d)"))))

(deftest slurp-barf-test
  (is (= "(ns foo)\n\n(println| \"Oiii\")" (run paredit/forward-slurp "(ns foo)\n\n(println|) \"Oiii\"")))
  (is (= "(a (b| c) d)" (run paredit/forward-slurp "(a (b|) c d)")))
  (is (= "(a (|b))" (run paredit/forward-slurp "(a (|) b)")))
  (is (= :default (run paredit/forward-slurp "(a (b|))")))
  (is (= "(a (b c|))" (run paredit/backward-slurp "(a b (c|))")))
  (is (= "(a|)" (run paredit/backward-slurp "a (|)")))
  (is (= "(a #{b c|})" (run paredit/backward-slurp "(a b #{c|})")))
  (is (= "(a b) |c" (run paredit/forward-barf "(a b |c)")))
  (is (= "() |a" (run paredit/forward-barf "(|a)")))
  (is (= "a (|b c)" (run paredit/backward-barf "(a |b c)")))
  (is (= "a (|)" (run paredit/backward-barf "(a|)")))
  (is (= :default (run paredit/forward-barf "(|)"))))

(deftest raise-kill-test
  (is (= "(a |c)" (run paredit/raise-sexp "(a (b |c))")))
  (is (= "(a |(c d))" (run paredit/raise-sexp "(a (b |(c d)))")))
  (is (= "(a |c)" (run paredit/kill-sexp "(a |b c)")))
  (is (= "(a |)" (run paredit/kill-sexp "(a |(b c))")))
  (is (= "b " (:killed (paredit/kill-sexp "(a b c)" 3)))))

(deftest move-over-forms-test
  (is (= "(a (b c)| d)" (run paredit/forward-sexp "(a |(b c) d)")))
  (is (= "(a b  c|)" (run paredit/forward-sexp "(a b|  c)")))
  (is (= :default (run paredit/forward-sexp "(a b|)")))
  (is (= "(a |(b c) d)" (run paredit/backward-sexp "(a (b c)| d)")))
  (is (= "(|ab c)" (run paredit/backward-sexp "(a|b c)")))
  (is (= [3 8] (:selection (paredit/forward-select "(a (b c) d)" 3 3))))
  (is (= [3 9] (:selection (paredit/backward-select "(a (b c) d)" 9 9)))))
