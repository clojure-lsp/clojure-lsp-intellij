(ns com.github.clojure-lsp.intellij.extension.completion
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.CompletionContributor
   :extends com.intellij.codeInsight.completion.CompletionContributor
   :exposes-methods {fillCompletionVariants fillCompletionVariantsSuper})
  (:require
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.ericdallo.clj4intellij.logger :as logger])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.codeInsight.completion CompletionParameters CompletionResultSet]
   [com.intellij.codeInsight.lookup AutoCompletionPolicy]
   [com.intellij.codeInsight.lookup LookupElementBuilder]
   [com.intellij.openapi.progress ProcessCanceledException]))

(set! *warn-on-reflection* true)

(def completion-kinds
  {1 :text 2 :method 3 :function 4 :constructor 5 :field 6 :variable 7 :class 8 :interface 9 :module 10 :property
   11 :unit 12 :value 13 :enum 14 :keyword 15 :snippet 16 :color 17 :file 18 :reference 19 :folder
   20 :enummember 21 :constant 22 :struct 23 :event 24 :operator 25 :typeparameter})

(defn ^:private completion-kind->icon [kind]
  (case (get completion-kinds kind)
    :class Icons/SYMBOL_CLASS
    :color Icons/SYMBOL_COLOR
    :constant Icons/SYMBOL_CONSTANT
    :constructor Icons/SYMBOL_METHOD
    :enum-mber Icons/SYMBOL_ENUMERATOR_MEMBER
    :enum Icons/SYMBOL_ENUMERATOR
    :event Icons/SYMBOL_EVENT
    :field Icons/SYMBOL_FIELD
    :file Icons/SYMBOL_FILE
    :interface Icons/SYMBOL_INTERFACE
    :keyword Icons/SYMBOL_KEYWORD
    :method Icons/SYMBOL_METHOD
    :function Icons/SYMBOL_METHOD
    :module Icons/SYMBOL_NAMESPACE
    :numeric Icons/SYMBOL_NUMERIC
    :operator Icons/SYMBOL_OPERATOR
    :property Icons/SYMBOL_PROPERTY
    :reference Icons/SYMBOL_REFERENCES
    :snippet Icons/SYMBOL_SNIPPET
    :string Icons/SYMBOL_STRING
    :struct Icons/SYMBOL_STRUCTURE
    :text Icons/SYMBOL_KEY
    :type-parameter Icons/SYMBOL_PARAMETER
    :unit Icons/SYMBOL_RULER
    :value Icons/SYMBOL_ENUMERATOR
    :variable Icons/SYMBOL_VARIABLE
    Icons/SYMBOL_MISC))

(def tag-number->tag {1 :deprecated})

(defn ^:private completion-item->lookup-element [{:keys [label kind detail tags]}]
  (let [normalized-label (if-let [i (string/index-of label "/")]
                           (subs label (inc i))
                           label)]
    (cond-> (LookupElementBuilder/create normalized-label)

      (some #(identical? :deprecated %) (mapv tag-number->tag tags))
      (.strikeout)

      :always
      (->
       (.withTypeText (or detail "") true)
       (.withPresentableText label)
       (.withIcon (completion-kind->icon kind))
       (.withAutoCompletionPolicy AutoCompletionPolicy/SETTINGS_DEPENDENT)))))

(defn -fillCompletionVariants [_ ^CompletionParameters params ^CompletionResultSet result]
  (when-let [client (lsp-client/connected-client)]
    (try
      (let [file (.getOriginalFile params)
            uri (.getUrl (.getVirtualFile file))
            editor (.getEditor params)
            [line character] (editor/offset->cursor-position editor (.getOffset params))
            items @(lsp-client/request! client [:textDocument/completion
                                                {:text-document {:uri uri}
                                                 :position {:line line :character character}}])]
        (.addAllElements result (mapv completion-item->lookup-element items)))
      (catch ProcessCanceledException _ignore)
      (catch Exception e
        (logger/error "Completion exception:" e)))))
