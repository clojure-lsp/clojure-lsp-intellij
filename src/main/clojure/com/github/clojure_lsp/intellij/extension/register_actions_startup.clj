(ns com.github.clojure-lsp.intellij.extension.register-actions-startup
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.RegisterActionsStartup
   :implements [com.intellij.openapi.startup.StartupActivity
                com.intellij.openapi.project.DumbAware])
  (:require
   [camel-snake-kebab.core :as csk]
   [com.github.clojure-lsp.intellij.action.implementations :as a.implementations]
   [com.github.clojure-lsp.intellij.action.refactors :as a.refactors]
   [com.github.clojure-lsp.intellij.action.references :as a.references]
   [com.github.ericdallo.clj4intellij.action :as action]
   [com.github.ericdallo.clj4intellij.logger :as logger])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.openapi.project Project]))

(set! *warn-on-reflection* true)

(def clojure-lsp-refactors
  [{:name "add-missing-import" :text "Add import to namespace" :description "Add import to namespace"}
   {:name "add-missing-libspec" :text "Add missing require" :description "Add missing require"}
   {:name "add-require-suggestion" :text "Add require suggestion" :description "Add require suggestion"}
   {:name "cycle-coll" :text "Cycle collection" :description "Cycle collections () [] {}"}
   {:name "cycle-keyword-auto-resolve" :text "Cycle keyword auto-resolve" :description "Cycle keyword auto-resolve"}
   {:name "clean-ns" :text "Clean namespace" :description "Clean current namespace, sorting and removing unused requires and imports" :use-shortcut-of "OptimizeImports"}
   {:name "cycle-privacy" :text "Cycle privacy" :description "Cycle privacy of def/defn"}
   {:name "create-test" :text "Create test" :description "Create test"}
   {:name "drag-param-backward" :text "Drag param backward" :description "Drag param backward"}
   {:name "drag-param-forward" :text "Drag param forward" :description "Drag param forward"}
   {:name "drag-backward" :text "Drag backward" :description "Drag backward" :use-shortcut-of "MoveStatementUp"}
   {:name "drag-forward" :text "Drag forward" :description "Drag forward" :use-shortcut-of "MoveStatementDown"}
   {:name "demote-fn" :text "Demote fn to #()" :description "Demote fn to #()"}
   {:name "destructure-keys" :text "Destructure keys" :description "Destructure keys"}
   {:name "extract-to-def" :text "Extract to def" :description "Extract to def"}
   {:name "extract-function" :text "Extract function" :description "Extract function"}
   {:name "expand-let" :text "Expand let" :description "Expand let"}
   {:name "create-function" :text "Create function" :description "Create function from example"}
   {:name "get-in-all" :text "Move to get/get-in" :description "Move all expressions to get/get-in"}
   {:name "get-in-less" :text "Remove from get/get-in" :description "Remove one element from get/get-in"}
   {:name "get-in-more" :text "Move all to get/get-in" :description "Move another expression to get/get-in"}
   {:name "get-in-none" :text "Unwind whole get/get-in" :description "Unwind whole get/get-in"}
   {:name "introduce-let" :text "Introduce let" :description "Introduce let"}
   {:name "inline-symbol" :text "Inline Symbol" :description "Inline Symbol"}
   {:name "resolve-macro-as" :text "Resolve macro as..." :description "Resolve macro as some existing macro or function"}
   {:name "move-form" :text "Move form" :description "Move form to another place"}
   {:name "move-to-let" :text "Move expression to let" :description "Move expression to let"}
   {:name "promote-fn" :text "Promote #() to fn, or fn to defn" :description "Promote #() to fn, or fn to defn"}
   {:name "replace-refer-all-with-refer" :text "Replace ':refer :all' with ':refer'" :description "Replace ':refer :all' with ':refer [...]'"}
   {:name "replace-refer-all-with-alias" :text "Replace ':refer :all' with alias" :description "Replace ':refer :all' with alias"}
   {:name "restructure-keys" :text "Restructure keys" :description "Restructure keys"}
   {:name "change-coll" :text "Switch collection to `{}, (), #{}, []`" :description "Switch collection to `{}, (), #{}, []`"}
   {:name "sort-clauses" :text "Sort map/vector/list/set/clauses" :description "Sort map/vector/list/set/clauses"}
   {:name "thread-first-all" :text "Thread first all" :description "Thread first all"}
   {:name "thread-first" :text "Thread first expression" :description "Thread first expression"}
   {:name "thread-last-all" :text "Thread last all" :description "Thread last all"}
   {:name "thread-last" :text "Thread last expression" :description "Thread last expression"}
   {:name "unwind-all" :text "Unwind whole thread" :description "Unwind whole thread"}
   {:name "unwind-thread" :text "Unwind thread once" :description "Unwind thread once"}
   {:name "forward-slurp" :text "Slurp forward" :description "Slurp forward" :keyboard-shortcut {:first "alt CLOSE_BRACKET" :replace-all true}}
   {:name "forward-barf" :text "Barf forward" :description "Barf forward" :keyboard-shortcut {:first "alt OPEN_BRACKET" :replace-all true}}
   {:name "backward-slurp" :text "Slurp backward" :description "Slurp backward" :keyboard-shortcut {:first "alt shift CLOSE_BRACKET" :replace-all true}}
   {:name "backward-barf" :text "Barf backward" :description "Barf backward" :keyboard-shortcut {:first "alt shift OPEN_BRACKET" :replace-all true}}
   {:name "raise-sexp" :text "Raise sexpr" :description "Raise current sexpr" :keyboard-shortcut {:first "alt R" :replace-all true}}
   {:name "kill-sexp" :text "Kill sexpr" :description "Kill current sexpr" :keyboard-shortcut {:first "alt K" :replace-all true}}])

(defn -runActivity [_this ^Project _project]
  (action/register-action! :id "ClojureLSP.FindReferences"
                           :title "Find references"
                           :description "Find all references of the element at cursor."
                           :icon Icons/CLOJURE
                           :use-shortcut-of "FindUsages"
                           :on-performed a.references/find-references-action)
  (action/register-action! :id "ClojureLSP.FindImplementations"
                           :title "Find implementations"
                           :description "Find all implementations of the element at cursor if any."
                           :icon Icons/CLOJURE
                           :use-shortcut-of "GotoImplementation"
                           :on-performed a.implementations/find-implementations-action)
  (action/register-group! :id "ClojureLSP.Find"
                          :children [{:type :add-to-group :group-id "EditorPopupMenu.GoTo" :anchor :first}
                                     {:type :add-to-group :group-id "GoToMenu" :anchor :before :relative-to "GotoDeclaration"}
                                     {:type :reference :ref "ClojureLSP.FindReferences"}
                                     {:type :reference :ref "ClojureLSP.FindImplementations"}
                                     {:type :separator}])
  (doseq [{:keys [name text description use-shortcut-of keyboard-shortcut]} clojure-lsp-refactors]
    (action/register-action! :id (str "ClojureLSP." (csk/->PascalCase name))
                             :title text
                             :description description
                             :icon Icons/CLOJURE
                             :keyboard-shortcut keyboard-shortcut
                             :use-shortcut-of use-shortcut-of
                             :on-performed (partial #'a.refactors/execute-refactor-action name)))

  (action/register-group! :id "ClojureLSP.Refactors"
                          :popup true
                          :text "Clojure refactors"
                          :icon Icons/CLOJURE
                          :children (concat [{:type :add-to-group :group-id "RefactoringMenu" :anchor :first}]
                                            (mapv (fn [{:keys [name]}] {:type :reference :ref (str "ClojureLSP." (csk/->PascalCase name))}) clojure-lsp-refactors)
                                            [{:type :separator}]))
  (logger/info "Actions registered"))
