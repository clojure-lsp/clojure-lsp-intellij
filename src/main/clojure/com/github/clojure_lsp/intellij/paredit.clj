(ns com.github.clojure-lsp.intellij.paredit
  "Structural editing (paredit) over plain text, independent of IntelliJ.

   Every operation takes the document text and the caret offset and returns
   either nil, meaning \"not handled, let the editor do its default\", or
   `{:edits [[start end replacement] ...] :caret offset}` where edits refer to
   the original text and caret is the offset after applying them."
  (:require
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(def ^:private pairs {\( \) \[ \] \{ \}})

(def ^:private closers #{\) \] \}})

(defn ^:private whitespace? [c]
  (let [c (char c)]
    (or (= \, c) (Character/isWhitespace c))))

(defn ^:private delimiter? [c]
  (let [c (char c)]
    (case c
      (\( \) \[ \] \{ \} \" \;) true
      (or (= \, c) (Character/isWhitespace c)))))

(defn ^:private reader-prefix?
  "Tokens like #, #?, #?@, #_, ', `, ~, ~@, @ and ^ that belong to the form
   right after them."
  [token]
  (boolean (re-matches #"[#'`~@^?_]+" token)))

(defn ^:private scan-string-end
  "Returns [end closed?] of the string whose opening quote is at `i`."
  [^String text i n]
  (loop [k (inc i)]
    (cond
      (>= k n) [n false]
      (= \\ (.charAt text k)) (recur (+ k 2))
      (= \" (.charAt text k)) [(inc k) true]
      :else (recur (inc k)))))

(defn ^:private scan-atom-end [^String text i n]
  (loop [k (if (= \\ (.charAt text i)) (min n (+ i 2)) (inc i))]
    (if (or (>= k n) (delimiter? (.charAt text k)))
      k
      (recur (inc k)))))

(defn ^:private skip-coll
  "Finds the end of the coll whose opening bracket is at `i` without building
   nodes. Returns [end closed? errors], where errors counts the unclosed and
   unmatched brackets inside it (not the coll itself) the same way `parse`
   does."
  [^String text i n]
  (let [expected (StringBuilder.)]
    (.append expected (char (pairs (.charAt text i))))
    (loop [k (inc i)
           errors 0]
      (if (>= k n)
        [n false (+ errors (dec (.length expected)))]
        (let [c (.charAt text k)]
          (cond
            (whitespace? c) (recur (inc k) errors)
            (= \; c) (recur (long (or (str/index-of text "\n" k) n)) errors)
            (= \" c) (let [[end closed?] (scan-string-end text k n)]
                       (recur (long end) (if closed? errors (inc errors))))
            (contains? pairs c) (do (.append expected (char (pairs c)))
                                    (recur (inc k) errors))
            (contains? closers c) (if (= c (.charAt expected (dec (.length expected))))
                                    (do (.setLength expected (dec (.length expected)))
                                        (if (zero? (.length expected))
                                          [(inc k) true errors]
                                          (recur (inc k) errors)))
                                    (recur (inc k) (inc errors)))
            :else (recur (long (scan-atom-end text k n)) errors)))))))

(defn ^:private add-child [stack node]
  (update-in stack [(dec (count stack)) :children] conj node))

(defn parse
  "Parses `text` into a tree of nodes. Each node has :type (:root, :coll,
   :string, :comment, :atom or :unmatched), :start and :end (exclusive).
   Colls and strings also have :open-end (offset right after the opening
   delimiter, reader prefixes included) and :closed?.

   With a `from`-`to` focus, only the colls that touch it are parsed into
   children. The others are shallow: they keep their bounds and an :errors
   count but no children, which keeps typing fast in large files."
  ([text]
   (parse text nil nil))
  ([^String text from to]
   (let [n (.length text)]
     (loop [i 0
            stack [{:type :root :start 0 :open-end 0 :closed? true :children []}]
            prefix-start nil]
       (if (>= i n)
         (loop [stack stack]
           (if (= 1 (count stack))
             (assoc (peek stack) :end n)
             (recur (add-child (pop stack) (assoc (peek stack) :end n :closed? false)))))
         (let [c (.charAt text i)
               start (or prefix-start i)]
           (cond
             (whitespace? c)
             (recur (inc i) stack nil)

             (= \; c)
             (let [end (or (str/index-of text "\n" i) n)]
               (recur (long end) (add-child stack {:type :comment :start i :end end}) nil))

             (= \" c)
             (let [[end closed?] (scan-string-end text i n)]
               (recur (long end) (add-child stack {:type :string :start start :open-end (inc i) :end end :closed? closed?}) nil))

             (contains? pairs c)
             (let [node {:type :coll :open c :start start :open-end (inc i) :children []}]
               (if from
                 (let [[end closed? errors] (skip-coll text i n)]
                   (if (or (> start to) (< end from))
                     (recur (long end) (add-child stack (assoc node :end end :closed? closed? :errors errors)) nil)
                     (recur (inc i) (conj stack node) nil)))
                 (recur (inc i) (conj stack node) nil)))

             (contains? closers c)
             (let [top (peek stack)]
               (if (and (= :coll (:type top)) (= c (char (pairs (:open top)))))
                 (recur (inc i) (add-child (pop stack) (assoc top :end (inc i) :closed? true)) nil)
                 (recur (inc i) (add-child stack {:type :unmatched :start i :end (inc i)}) nil)))

             :else
             (let [end (scan-atom-end text i n)]
               (if (and (< end n)
                        (let [next-c (.charAt text end)]
                          (or (contains? pairs next-c) (= \" next-c)))
                        (reader-prefix? (subs text i end)))
                 (recur (long end) stack start)
                 (recur (long end) (add-child stack {:type :atom :start start :end end}) nil))))))))))

(defn ^:private error-count
  "Number of unclosed or unmatched delimiters in the tree."
  [node]
  (+ (if (or (= :unmatched (:type node))
             (and (#{:coll :string} (:type node)) (not (:closed? node))))
       1 0)
     (:errors node 0)
     (reduce + 0 (map error-count (:children node)))))

(defn ^:private structure-errors [text]
  (error-count (parse text -1 -1)))

(defn ^:private apply-edits* [^String text edits]
  (reduce (fn [^String t [start end replacement]]
            (str (subs t 0 start) replacement (subs t end)))
          text
          (sort-by first > edits)))

(defn apply-edits
  "Applies an operation result to `text`, returning [new-text caret]."
  [text {:keys [edits caret]}]
  [(apply-edits* text edits) caret])

(defn ^:private keeps-balance? [text edits]
  (<= (structure-errors (apply-edits* text edits))
      (structure-errors text)))

(declare path-at)

(defn ^:private whole-region?
  "True when the region holds whole forms, or lies inside a single string or
   comment, so removing or wrapping it cannot break the structure."
  [text start end]
  (let [root (parse text start end)
        node-at #(peek (path-at root %))]
    (or (and (#{:string :comment} (:type (node-at start)))
             (= (node-at start) (node-at end)))
        (and (zero? (structure-errors (subs text start end)))
             (keeps-balance? text [[start end ""]])))))

(defn ^:private inside? [{:keys [type start end open-end closed?]} offset]
  (case type
    (:coll :string) (and (<= open-end offset) (if closed? (< offset end) (<= offset end)))
    :comment (and (< start offset) (<= offset end))
    false))

(defn ^:private path-at
  "Nodes from the root down to the innermost coll, string or comment that
   contains `offset`."
  [root offset]
  (loop [path [root]]
    (if-let [child (some #(when (inside? % offset) %) (:children (peek path)))]
      (recur (conj path child))
      path)))

(defn ^:private context [text offset]
  (peek (path-at (parse text offset offset) offset)))

(defn ^:private structural? [node]
  (#{:coll :root} (:type node)))

(defn ^:private closer-start [{:keys [end closed?]}]
  (if closed? (dec end) end))

(defn ^:private empty-node? [^String text {:keys [type open-end end closed? children]}]
  (and closed?
       (case type
         :string (= open-end (dec end))
         :coll (and (empty? children) (str/blank? (subs text open-end (dec end))))
         false)))

(defn ^:private short-char-literal
  "The `\\x` char literal among `children` whose span touches `offset`, when
   x is a delimiter: deleting only one of its two chars breaks the balance."
  [^String text children offset]
  (some (fn [{:keys [type start end] :as node}]
          (when (and (= :atom type)
                     (= \\ (.charAt text start))
                     (> end (inc start))
                     (delimiter? (.charAt text (inc start)))
                     (<= start offset (+ start 2)))
            node))
        children))

(defn ^:private pending-char-literal? [^String text node offset]
  (boolean (some #(and (= :atom (:type %))
                       (= (:start %) (dec offset))
                       (= \\ (.charAt text (:start %))))
                 (:children node))))

(defn ^:private escape-at
  "Start of the escape sequence (backslash plus one char) inside `string`
   that covers the char at index `i`, if any."
  [^String text string i]
  (loop [k (:open-end string)]
    (cond
      (> k i) nil
      (= \\ (.charAt text k)) (if (<= k i (inc k)) k (recur (+ k 2)))
      :else (recur (inc k)))))

(defn ^:private move [offset]
  {:edits [] :caret offset})

(defn ^:private delete-range [start end]
  {:edits [[start end ""]] :caret start})

(def ^:private noop-at move)

;; Typing

(defn insert-pair
  "Typing an opening bracket inserts the balanced pair."
  [text offset open]
  (let [node (context text offset)]
    (when (and (structural? node) (not (pending-char-literal? text node offset)))
      {:edits [[offset offset (str open (pairs open))]] :caret (inc offset)})))

(defn insert-closer
  "Typing a closing bracket moves past the end of the enclosing form instead
   of inserting a new, unbalanced one."
  [text offset]
  (let [node (context text offset)]
    (when (and (structural? node) (not (pending-char-literal? text node offset)))
      (cond
        (= :root (:type node)) (noop-at offset)
        (:closed? node) (move (:end node))
        :else nil))))

(defn insert-quote
  "Typing a double quote inserts an empty string, steps over the closing
   quote, or escapes the quote inside a string."
  [^String text offset]
  (let [node (context text offset)]
    (case (:type node)
      :comment nil
      :string (cond
                (and (:closed? node) (= offset (dec (:end node)))) (move (:end node))
                (escape-at text node (dec offset)) nil
                (:closed? node) {:edits [[offset offset "\\\""]] :caret (+ offset 2)}
                :else nil)
      (when-not (pending-char-literal? text node offset)
        {:edits [[offset offset "\"\""]] :caret (inc offset)}))))

(defn insert-semicolon
  "Typing a semicolon that would comment out closing brackets starts the
   comment and moves the rest of the line to the next one."
  [text offset]
  (let [node (context text offset)]
    (when (and (structural? node)
               (not (pending-char-literal? text node offset))
               (not (keeps-balance? text [[offset offset ";"]])))
      {:edits [[offset offset ";\n"]] :caret (inc offset)})))

(defn insert-backslash
  "Typing a backslash right before a bracket, quote or semicolon would turn
   it into a char literal, so a space is inserted after the backslash."
  [^String text offset]
  (let [node (context text offset)]
    (when (and (structural? node)
               (not (pending-char-literal? text node offset))
               (< offset (.length text))
               (contains? #{\( \) \[ \] \{ \} \" \;} (.charAt text offset)))
      {:edits [[offset offset "\\ "]] :caret (inc offset)})))

(defn replace-selection
  "Typing over a selection replaces it only when it holds whole forms."
  [text start end]
  (when-not (whole-region? text start end)
    (noop-at end)))

(defn replacement-keeps-balance?
  "True when replacing the region with `replacement` keeps the brackets
   balanced."
  [text start end replacement]
  (keeps-balance? text [[start end replacement]]))

(defn change-keeps-balance?
  "True when going from `before` to `after` does not unbalance anything."
  [before after]
  (<= (structure-errors after) (structure-errors before)))

(defn restoring-edit
  "The single edit [start end replacement] over `from` that turns it into
   `to`, found by skipping their common prefix and suffix."
  [^String from ^String to]
  (let [n (min (.length from) (.length to))
        prefix (loop [i 0]
                 (if (and (< i n) (= (.charAt from i) (.charAt to i))) (recur (inc i)) i))
        suffix (loop [i 0]
                 (if (and (< i (- n prefix))
                          (= (.charAt from (- (.length from) 1 i)) (.charAt to (- (.length to) 1 i))))
                   (recur (inc i))
                   i))]
    [prefix (- (.length from) suffix) (subs to prefix (- (.length to) suffix))]))

(defn region-removable?
  "True when deleting the region keeps the brackets balanced."
  [text start end]
  (whole-region? text start end))

;; Deleting

(defn ^:private guard-default
  "When no rule applies, allow the plain single char deletion only if it
   keeps the brackets balanced."
  [^String text start end]
  (let [c (.charAt text start)]
    (if (and (or (delimiter? c) (= \\ c) (= \# c)
                 (and (pos? start) (= \\ (.charAt text (dec start)))))
             (not (keeps-balance? text [[start end ""]])))
      (noop-at (if (= end (inc start)) start end))
      nil)))

(defn backspace
  "Deletes the char before the caret without breaking the balance."
  [^String text offset]
  (when (pos? offset)
    (let [node (context text offset)]
      (case (:type node)
        :comment nil

        :string
        (cond
          (= offset (:open-end node))
          (if (empty-node? text node)
            (delete-range (:start node) (:end node))
            (move (:start node)))

          (escape-at text node (dec offset))
          (let [esc (escape-at text node (dec offset))]
            (delete-range esc (min (+ esc 2) (closer-start node))))

          :else nil)

        (let [children (:children node)
              before (some #(when (= (:end %) offset) %) children)
              char-literal (short-char-literal text children offset)]
          (cond
            (and (= :coll (:type node)) (= offset (:open-end node)))
            (if (empty-node? text node)
              (delete-range (:start node) (:end node))
              (move (:start node)))

            (and before (#{:coll :string} (:type before)) (:closed? before))
            (if (empty-node? text before)
              (delete-range (:start before) (:end before))
              (move (dec offset)))

            (and char-literal (< (:start char-literal) offset))
            (delete-range (:start char-literal) (+ 2 (:start char-literal)))

            :else (guard-default text (dec offset) offset)))))))

(defn delete-forward
  "Deletes the char after the caret without breaking the balance."
  [^String text offset]
  (when (< offset (.length text))
    (let [node (context text offset)]
      (case (:type node)
        :comment nil

        :string
        (cond
          (and (:closed? node) (= offset (dec (:end node))))
          (if (empty-node? text node)
            (delete-range (:start node) (:end node))
            (move (:end node)))

          (escape-at text node offset)
          (let [esc (escape-at text node offset)]
            (delete-range esc (min (+ esc 2) (closer-start node))))

          :else nil)

        (let [children (:children node)
              after (some #(when (= (:start %) offset) %) children)
              char-literal (short-char-literal text children offset)]
          (cond
            (and (= :coll (:type node)) (:closed? node) (= offset (dec (:end node))))
            (if (empty-node? text node)
              (delete-range (:start node) (:end node))
              (move (:end node)))

            (and after (#{:coll :string} (:type after)) (:closed? after))
            (if (empty-node? text after)
              (delete-range (:start after) (:end after))
              (move (:open-end after)))

            (and char-literal (< offset (+ 2 (:start char-literal))))
            (delete-range (:start char-literal) (+ 2 (:start char-literal)))

            :else (guard-default text offset (inc offset))))))))

(defn delete-selection
  "Deletes the selected text only when that keeps the brackets balanced."
  [text start end]
  (if (whole-region? text start end)
    (delete-range start end)
    (noop-at end)))

(defn kill
  "Kills from the caret to the end of the line, extending over whole forms
   so the brackets stay balanced. The result has the killed text in :killed."
  [^String text offset]
  (let [node (context text offset)
        n (.length text)
        line-end (or (str/index-of text "\n" offset) n)
        end (case (:type node)
              :comment (:end node)
              :string (min line-end (closer-start node))
              (let [ends (keep #(when (and (> (:end %) offset) (< (:start %) line-end)) (:end %))
                               (:children node))
                    closer (closer-start node)]
                (cond
                  (seq ends) (apply max ends)
                  (< line-end closer) (inc line-end)
                  :else closer)))]
    (when (> end offset)
      (assoc (delete-range offset end) :killed (subs text offset end)))))

;; Wrapping, splicing, splitting and joining

(defn ^:private form-at [children offset]
  (or (some #(when (and (<= (:start %) offset) (< offset (:end %))) %) children)
      (some #(when (>= (:start %) offset) %) children)))

(defn wrap
  "Wraps the selection, or the form at or after the caret, with `open` and
   its closing bracket."
  [text sel-start sel-end open]
  (let [close (str (pairs open))]
    (if (< sel-start sel-end)
      (when (whole-region? text sel-start sel-end)
        {:edits [[sel-end sel-end close] [sel-start sel-start (str open)]]
         :caret (inc sel-start)})
      (let [node (context text sel-start)]
        (when (structural? node)
          (if-let [{:keys [start end]} (form-at (remove #(= :comment (:type %)) (:children node)) sel-start)]
            {:edits [[end end close] [start start (str open)]] :caret (inc start)}
            {:edits [[sel-start sel-start (str open close)]] :caret (inc sel-start)}))))))

(defn ^:private enclosing-coll [text offset]
  (let [node (context text offset)]
    (when (and (= :coll (:type node)) (:closed? node))
      node)))

(defn splice
  "Removes the delimiters of the enclosing form, keeping its contents."
  [text offset]
  (when-let [{:keys [start open-end end]} (enclosing-coll text offset)]
    {:edits [[(dec end) end ""] [start open-end ""]]
     :caret (- offset (- open-end start))}))

(defn splice-killing-backward
  "Kills the forms before the caret, then splices the enclosing form."
  [text offset]
  (when-let [{:keys [start open-end end children]} (enclosing-coll text offset)]
    (let [cut (or (some #(when (< (:start %) offset (:end %)) (:start %)) children) offset)]
      {:edits [[(dec end) end ""] [start cut ""]]
       :caret start})))

(defn splice-killing-forward
  "Kills the forms after the caret, then splices the enclosing form."
  [text offset]
  (when-let [{:keys [start open-end end children]} (enclosing-coll text offset)]
    (let [cut (or (some #(when (< (:start %) offset (:end %)) (:end %)) children) offset)]
      {:edits [[cut end ""] [start open-end ""]]
       :caret (- (max offset open-end) (- open-end start))})))

(defn split
  "Splits the enclosing form or string in two at the caret."
  [^String text offset]
  (let [node (context text offset)
        {:keys [type start open-end closed?]} node
        opening (when (#{:coll :string} type) (subs text start open-end))]
    (when (and opening closed?)
      (case type
        :string
        (when-not (escape-at text node (dec offset))
          {:edits [[offset offset (str "\" " opening)]] :caret (inc offset)})

        :coll
        (let [closer (dec (:end node))
              left (loop [k offset] (if (and (> k open-end) (whitespace? (.charAt text (dec k)))) (recur (dec k)) k))
              right (loop [k offset] (if (and (< k closer) (whitespace? (.charAt text k))) (recur (inc k)) k))
              gap (subs text left right)
              separator (if-let [newline (str/index-of gap "\n")] (subs gap newline) " ")]
          {:edits [[left right (str (.charAt text closer) separator opening)]]
           :caret (inc left)})))))

(defn join
  "Joins the two forms or strings around the caret into one."
  [^String text offset]
  (let [node (context text offset)]
    (when (structural? node)
      (let [children (:children node)
            prev (last (filter #(<= (:end %) offset) children))
            next (first (filter #(>= (:start %) offset) children))
            opening #(subs text (:start %) (:open-end %))]
        (when (and prev next
                   (= (:type prev) (:type next))
                   (#{:coll :string} (:type prev))
                   (:closed? prev) (:closed? next)
                   (= (opening prev) (opening next))
                   (str/blank? (subs text (:end prev) (:start next))))
          (let [junction (dec (:end prev))
                separator (if (or (= :string (:type prev)) (empty-node? text prev) (empty-node? text next)) "" " ")]
            {:edits [[junction (:open-end next) separator]]
             :caret junction}))))))

;; Navigation

(defn backward-up
  "Moves the caret to the start of the enclosing form."
  [text offset]
  (let [node (last (filter #(#{:coll :string} (:type %)) (path-at (parse text offset offset) offset)))]
    (when node (move (:start node)))))

(defn forward-up
  "Moves the caret past the end of the enclosing form."
  [text offset]
  (let [node (last (filter #(#{:coll :string} (:type %)) (path-at (parse text offset offset) offset)))]
    (when (:closed? node) (move (:end node)))))

(defn forward-down
  "Moves the caret into the next form after it."
  [text offset]
  (let [node (context text offset)]
    (when (structural? node)
      (when-let [child (some #(when (and (= :coll (:type %)) (>= (:start %) offset)) %) (:children node))]
        (move (:open-end child))))))

(defn backward-down
  "Moves the caret into the end of the previous form before it."
  [text offset]
  (let [node (context text offset)]
    (when (structural? node)
      (when-let [child (last (filter #(and (= :coll (:type %)) (:closed? %) (<= (:end %) offset)) (:children node)))]
        (move (dec (:end child)))))))

;; Slurping, barfing, raising and moving over forms

(defn ^:private shift-caret
  "Where a caret at `offset` ends up after applying `edits`."
  [offset edits]
  (+ offset (reduce + 0 (keep (fn [[start end ^String replacement]]
                                (when (<= end offset)
                                  (- (.length replacement) (- end start))))
                              edits))))

(defn ^:private result [offset edits]
  {:edits edits :caret (shift-caret offset edits)})

(defn ^:private sexps
  "The children of `node` that are forms, skipping comments."
  [node]
  (remove #(= :comment (:type %)) (:children node)))

(defn ^:private coll-path
  "Nodes from the root down to the innermost closed coll containing `offset`."
  [text offset]
  (let [path (path-at (parse text offset offset) offset)
        index (last (keep-indexed (fn [i node] (when (and (= :coll (:type node)) (:closed? node)) i)) path))]
    (when index
      (subvec path 0 (inc index)))))

(defn forward-slurp
  "Moves the closing bracket of the current form past the next form."
  [^String text offset]
  (when-let [path (coll-path text offset)]
    (let [coll (peek path)
          next-form (first (filter #(>= (:start %) (:end coll)) (sexps (peek (pop path)))))
          closer (dec (:end coll))]
      (when next-form
        (result offset [[(:end next-form) (:end next-form) (str (.charAt text closer))]
                        [closer (if (empty? (sexps coll)) (:start next-form) (inc closer)) ""]])))))

(defn backward-slurp
  "Moves the opening bracket of the current form before the previous form."
  [^String text offset]
  (when-let [path (coll-path text offset)]
    (let [{:keys [start open-end] :as coll} (peek path)
          prev-form (last (filter #(<= (:end %) start) (sexps (peek (pop path)))))]
      (when prev-form
        (result offset [[(if (empty? (sexps coll)) (:end prev-form) start) open-end ""]
                        [(:start prev-form) (:start prev-form) (subs text start open-end)]])))))

(defn forward-barf
  "Moves the closing bracket of the current form before its last form."
  [^String text offset]
  (when-let [{:keys [open-end end] :as coll} (peek (coll-path text offset))]
    (let [forms (sexps coll)
          close (str (.charAt text (dec end)))]
      (when (seq forms)
        (let [before (if-let [prev-form (last (butlast forms))] (:end prev-form) open-end)]
          (result offset [[(dec end) end ""]
                          [before before (if (next forms) close (str close " "))]]))))))

(defn backward-barf
  "Moves the opening bracket of the current form after its first form."
  [^String text offset]
  (when-let [{:keys [start open-end] :as coll} (peek (coll-path text offset))]
    (let [forms (sexps coll)
          opening (subs text start open-end)]
      (when (seq forms)
        (let [after (if-let [second-form (second forms)] (:start second-form) (:end (first forms)))]
          (result offset [[start open-end ""]
                          [after after (if (next forms) opening (str " " opening))]]))))))

(defn raise-sexp
  "Replaces the current form with the form at the caret."
  [text offset]
  (when-let [coll (peek (coll-path text offset))]
    (when-let [form (form-at (sexps coll) offset)]
      {:edits [[(:start coll) (:end coll) (subs text (:start form) (:end form))]]
       :caret (+ (:start coll) (max 0 (- offset (:start form))))})))

(defn kill-sexp
  "Kills the form at or after the caret. The result has the killed text in
   :killed."
  [^String text offset]
  (let [node (context text offset)]
    (when (structural? node)
      (when-let [{:keys [start end]} (form-at (sexps node) offset)]
        (let [end (if (and (< end (.length text)) (= \space (.charAt text end))) (inc end) end)]
          (assoc (delete-range start end) :killed (subs text start end)))))))

(defn forward-sexp
  "Moves the caret to the end of the form at or after it."
  [text offset]
  (let [node (context text offset)]
    (when (structural? node)
      (when-let [form (form-at (sexps node) offset)]
        (move (:end form))))))

(defn backward-sexp
  "Moves the caret to the start of the form at or before it."
  [text offset]
  (let [node (context text offset)]
    (when (structural? node)
      (when-let [form (last (filter #(< (:start %) offset) (sexps node)))]
        (move (:start form))))))

(defn forward-select
  "Extends the selection to the end of the next form."
  [text start end]
  (when-let [{:keys [caret]} (forward-sexp text end)]
    {:edits [] :caret caret :selection [start caret]}))

(defn backward-select
  "Extends the selection to the start of the previous form."
  [text start end]
  (when-let [{:keys [caret]} (backward-sexp text start)]
    {:edits [] :caret caret :selection [caret end]}))
