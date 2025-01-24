(ns com.github.clojure-lsp.intellij.extension.register-actions-startup
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.RegisterActionsStartup
   :implements [com.intellij.openapi.startup.StartupActivity
                com.intellij.openapi.project.DumbAware])
  (:require
   [com.github.ericdallo.clj4intellij.action :as action]
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.openapi.actionSystem ActionManager AnActionEvent KeyboardShortcut]
   [com.intellij.openapi.keymap KeymapManager]
   [com.intellij.openapi.project Project]
   [com.redhat.devtools.lsp4ij.commands CommandExecutor LSPCommandAction LSPCommandContext]
   [javax.swing Icon KeyStroke]
   [org.eclipse.lsp4j Command]))

(set! *warn-on-reflection* true)

(def clojure-lsp-commands
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
   {:name "kill-sexp" :text "Kill sexpr" :description "Kill current sexpr (Paredit)" :keyboard-shortcut {:first "alt K" :replace-all true}}])

#_{:clj-kondo/ignore [:unused-binding]}
(defn register-command!
  [& {:keys [project id title description icon args use-shortcut-of keyboard-shortcut on-performed]}]
  (let [manager (ActionManager/getInstance)
        keymap-manager (KeymapManager/getInstance)
        keymap (.getActiveKeymap keymap-manager)
        action (proxy+ ClojureLSPCommand [] LSPCommandAction
                       (commandPerformed [_ _command ^AnActionEvent event]
                                         (-> (CommandExecutor/executeCommand
                                               (doto (LSPCommandContext. (Command. title id) project)
                                                 (.setPreferredLanguageServerId "clojure-lsp")))
                                             (.response)
                                             )))]
    (.setText (.getTemplatePresentation action) ^String title)
    (.setIcon (.getTemplatePresentation action) ^Icon icon)
    (when-not (.getAction manager id)
      (.registerAction manager id action)
      (when use-shortcut-of
        (.addShortcut keymap
                      id
                      (first (.getShortcuts (.getShortcutSet (.getAction manager use-shortcut-of))))))
      (when keyboard-shortcut
        (let [k-shortcut (KeyboardShortcut. (KeyStroke/getKeyStroke ^String (:first keyboard-shortcut))
                                            (some-> ^String (:second keyboard-shortcut) KeyStroke/getKeyStroke))]
          (when (empty? (.getShortcuts keymap id))
            (.addShortcut keymap id k-shortcut))
          (when (:replace-all keyboard-shortcut)
            (doseq [[conflict-action-id shortcuts] (.getConflicts keymap id k-shortcut)]
              (doseq [shortcut shortcuts]
                (.removeShortcut keymap conflict-action-id shortcut))))))
      action)))

(defn -runActivity [_this ^Project project]
  (doseq [{:keys [name text args description use-shortcut-of keyboard-shortcut]} clojure-lsp-commands]
    (register-command! :id name
                       :project project
                       :title text
                       :description description
                       :args args
                       :icon Icons/CLOJURE
                       :keyboard-shortcut keyboard-shortcut
                       :use-shortcut-of use-shortcut-of))
  (action/register-group! :id "ClojureLSP.Refactors"
                          :popup true
                          :text "Clojure refactors"
                          :icon Icons/CLOJURE
                          :children (concat [{:type :add-to-group :group-id "RefactoringMenu" :anchor :first}]
                                            (mapv (fn [{:keys [name]}] {:type :reference :ref name}) clojure-lsp-commands)
                                            [{:type :separator}]))
  (logger/info "Actions registered"))
