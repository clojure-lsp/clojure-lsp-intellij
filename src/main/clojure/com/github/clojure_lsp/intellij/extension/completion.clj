(ns com.github.clojure-lsp.intellij.extension.completion
  (:gen-class
   ;; :post-init post-init
   :name com.github.clojure_lsp.intellij.extension.CompletionContributor
   :extends com.intellij.codeInsight.completion.CompletionContributor
   :exposes-methods {fillCompletionVariants fillCompletionVariantsSuper})
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.logger :as logger]
   [com.github.clojure-lsp.intellij.lsp-client :as lsp-client])
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
    ;; TODO use proper icons from LSP
    :function Icons/CLOJURE
    nil))

(defn ^:private completion-item->lookup-element [{:keys [label kind detail]}]
  (-> (LookupElementBuilder/create label)
      (.withTypeText (or detail "") true)
      ;; (.bold)
      ;; (.strikeout)
      (.withIcon (completion-kind->icon kind))
      ;; TODO underline when deprecated
      ;; (.withItemTextUnderlined true)
      ;; (.withItemTextItalic true)
      (.withAutoCompletionPolicy AutoCompletionPolicy/SETTINGS_DEPENDENT)))

(defn -fillCompletionVariants [_ ^CompletionParameters params ^CompletionResultSet result]
  (when-let [client (and (identical? :connected (:status @db/db*))
                         (:client @db/db*))]
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
        (logger/error "Completion exception: %s" e)))))
