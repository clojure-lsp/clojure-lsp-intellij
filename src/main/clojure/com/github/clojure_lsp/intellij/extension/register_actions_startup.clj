(ns com.github.clojure-lsp.intellij.extension.register-actions-startup
  (:require
   [camel-snake-kebab.core :as csk]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.extension.new-file :as extension.new-file]
   [com.github.ericdallo.clj4intellij.action :as action]
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]]
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [com.github.ericdallo.clj4intellij.tasks :as tasks]
   [com.github.ericdallo.clj4intellij.util :as util]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.google.gson JsonPrimitive]
   [com.intellij.openapi.actionSystem
    ActionManager
    ActionUpdateThread
    AnActionEvent
    CommonDataKeys]
   [com.intellij.openapi.editor Editor]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.startup ProjectActivity]
   [com.redhat.devtools.lsp4ij LanguageServerManager]
   [com.redhat.devtools.lsp4ij.commands LSPCommand LSPCommandAction]
   [com.redhat.devtools.lsp4ij.usages LSPUsageType LSPUsagesManager LocationData]
   [kotlinx.coroutines CoroutineScope]))

(set! *warn-on-reflection* true)

(def ^:private clojure-lsp-commands
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
   {:name "forward-slurp" :text "Slurp forward" :description "Slurp forward (Paredit)" :keyboard-shortcut {:first "alt CLOSE_BRACKET" :replace-all true}}
   {:name "forward-barf" :text "Barf forward" :description "Barf forward (Paredit)" :keyboard-shortcut {:first "alt OPEN_BRACKET" :replace-all true}}
   {:name "backward-slurp" :text "Slurp backward" :description "Slurp backward (Paredit)" :keyboard-shortcut {:first "alt shift CLOSE_BRACKET" :replace-all true}}
   {:name "backward-barf" :text "Barf backward" :description "Barf backward (Paredit)" :keyboard-shortcut {:first "alt shift OPEN_BRACKET" :replace-all true}}
   {:name "raise-sexp" :text "Raise sexpr" :description "Raise current sexpr (Paredit)" :keyboard-shortcut {:first "alt R" :replace-all true}}
   {:name "kill-sexp" :text "Kill sexpr" :description "Kill current sexpr (Paredit)" :keyboard-shortcut {:first "alt K" :replace-all true}}
   {:name "forward" :text "Move forward" :description "Move cursor forward a sexpr (Paredit)" :keyboard-shortcut {:first "ctrl alt CLOSE_BRACKET" :replace-all true}}
   {:name "forward-select" :text "Select forward" :description "Select forward a sexpr (Paredit)" :keyboard-shortcut {:first "ctrl shift alt CLOSE_BRACKET" :replace-all true}}
   {:name "backward" :text "Move backward" :description "Move cursor backward a sexpr (Paredit)" :keyboard-shortcut {:first "ctrl alt OPEN_BRACKET" :replace-all true}}
   {:name "backward-select" :text "Select backward" :description "Select backward a sexpr (Paredit)" :keyboard-shortcut {:first "ctrl shift alt OPEN_BRACKET" :replace-all true}}])

(defn ^:private on-action-performed [command-name text ^AnActionEvent event]
  (when-let [editor ^Editor (.getData event CommonDataKeys/EDITOR_EVEN_IF_INACTIVE)]
    (let [project (.getProject event)
          [line character] (util/editor->cursor-position editor)]
      (tasks/run-background-task!
       project
       "LSP: refactoring"
       (fn [_]
         (lsp-client/execute-command command-name text [(editor/editor->uri editor) line character] project))))))

(defn ^:private register-command!
  [& {:keys [id on-performed]}]
  (let [manager (ActionManager/getInstance)
        action (proxy+ [] LSPCommandAction
                 (commandPerformed [_ command event] (on-performed command event))
                 (getCommandPerformedThread [_] ActionUpdateThread/EDT))]
    (when-not (.getAction manager id)
      (.registerAction manager id action)
      action)))

(defn ^:private code-lens-references-performed [^LSPCommand command ^AnActionEvent event]
  (let [project (.getProject event)
        uri (.getAsString ^JsonPrimitive (.getArgumentAt command 0))
        line (dec (.getAsInt ^JsonPrimitive (.getArgumentAt command 1)))
        character (dec (.getAsInt ^JsonPrimitive (.getArgumentAt command 2)))
        references (lsp-client/references uri line character project)
        language-server @(.getLanguageServer (LanguageServerManager/getInstance project) "clojure-lsp")]
    (.findShowUsagesInPopup
     (LSPUsagesManager/getInstance project)
     (mapv #(LocationData. % language-server) references)
     LSPUsageType/References
     (.getDataContext event)
     (.getInputEvent event))))

(def-extension RegisterActionsStartup []
  ProjectActivity
  (execute [_this ^Project project ^CoroutineScope _]
    (action/register-action! :id "ClojureLSP.NewClojureFile"
                             :title "Clojure namespace"
                             :description "Create a Clojure namespace"
                             :icon Icons/CLOJURE
                             :action (extension.new-file/->ClojureNewFileAction project))
    (action/register-group! :id "ClojureLSP.NewGroup"
                            :children [{:type :add-to-group :group-id "NewGroup" :anchor :first}
                                       {:type :reference :ref "ClojureLSP.NewClojureFile"}])
    (doseq [{:keys [name text description use-shortcut-of keyboard-shortcut]} clojure-lsp-commands]
      (action/register-action! :id (str "ClojureLSP." (csk/->PascalCase name))
                               :title text
                               :description description
                               :icon Icons/CLOJURE
                               :keyboard-shortcut keyboard-shortcut
                               :use-shortcut-of use-shortcut-of
                               :on-performed (partial on-action-performed name text)))
    (register-command! :id "code-lens-references"
                       :on-performed #'code-lens-references-performed)
    (action/register-group! :id "ClojureLSP.Refactors"
                            :popup true
                            :text "Clojure refactors"
                            :icon Icons/CLOJURE
                            :children (concat [{:type :add-to-group :group-id "RefactoringMenu" :anchor :first}]
                                              (mapv (fn [{:keys [name]}] {:type :reference :ref (str "ClojureLSP." (csk/->PascalCase name))}) clojure-lsp-commands)
                                              [{:type :separator}]))
    (logger/info "Actions registered")))
