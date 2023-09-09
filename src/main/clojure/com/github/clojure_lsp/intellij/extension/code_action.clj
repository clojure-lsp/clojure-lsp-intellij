(ns com.github.clojure-lsp.intellij.extension.code-action
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.core.memoize :as memoize]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.server :as server])
  (:import
   [com.intellij.openapi.editor Editor]
   [com.intellij.openapi.project Project]
   [com.intellij.psi PsiFile]))

(set! *warn-on-reflection* true)

(defonce code-action-name->title* (atom {}))

(defn ^:private req-code-actions [uri [line character]]
  (when-let [client (server/connected-client)]
    (let [diagnostics (->> (get-in @db/db* [:diagnostics uri])
                           (filterv (fn [{{:keys [start end]} :range}]
                                      (and (<= (:line start) line (:line end))
                                           (<= (:character start) character (:character end))))))
          response @(lsp-client/request! client [:textDocument/codeAction
                                                 {:text-document {:uri uri}
                                                  :context {:diagnostics diagnostics}
                                                  :range {:start {:line line
                                                                  :character character}
                                                          :end {:start {:line line
                                                                        :character character}}}}])
          code-actions (reduce
                        (fn [a b]
                          (let [command-name (-> b :command :command)]
                            (swap! code-action-name->title* assoc command-name (:title b))
                            (assoc a
                                   command-name
                                   (assoc (:command b) :kind (:kind b)))))
                        {}
                        response)]
      code-actions)))

(def ^:private memoized-code-actions
  (memoize/ttl req-code-actions :ttl/threshold 1000))

(defn ^:private editor+command->code-action [^PsiFile file ^Editor editor command]
  (when-let [vfile (.getVirtualFile file)]
    (let [uri (.getUrl vfile)
          pos (editor/editor->cursor-position editor)]
      (get (memoized-code-actions uri pos) command))))

(defn ^:private is-available [name _ _project editor file]
  (boolean (editor+command->code-action file editor name)))

(defn ^:private get-text [name _]
  (get @code-action-name->title* name name))

(defn ^:private invoke [name _ ^Project _project editor file]
  (when-let [{:keys [command arguments]} (editor+command->code-action file editor name)]
    (let [client (:client @db/db*)]
      (lsp-client/request! client [:workspace/executeCommand
                                   {:command command
                                    :arguments arguments}]))))

(defmacro ^:private gen-code-action [& {:keys [name]}]
  `(do
     (def ~(symbol (str (str name "-") "getText")) (partial get-text ~name))
     (defn ~(symbol (str (str name "-") "getFamilyName")) [_#] "Clojure LSP code actions")
     (defn ~(symbol (str (str name "-") "startInWriteAction")) [_#] true)
     (def ~(symbol (str (str name "-") "isAvailable")) (partial is-available ~name))
     (def ~(symbol (str (str name "-") "invoke")) (partial invoke ~name))
     (gen-class
      :name ~(str "com.github.clojure_lsp.intellij.extension.code_action." (csk/->PascalCase name))
      :prefix ~(str name "-")
      :extends "com.intellij.codeInsight.intention.impl.BaseIntentionAction")))

(gen-code-action :name "add-missing-import")
(gen-code-action :name "add-missing-libspec")
(gen-code-action :name "add-require-suggestion")
(gen-code-action :name "cycle-coll")
(gen-code-action :name "cycle-keyword-auto-resolve")
(gen-code-action :name "clean-ns")
(gen-code-action :name "cycle-privacy")
(gen-code-action :name "create-test")
(gen-code-action :name "drag-param-backward")
(gen-code-action :name "drag-param-forward")
(gen-code-action :name "drag-backward")
(gen-code-action :name "drag-forward")
(gen-code-action :name "demote-fn")
(gen-code-action :name "destructure-keys")
(gen-code-action :name "extract-to-def")
(gen-code-action :name "extract-function")
(gen-code-action :name "expand-let")
(gen-code-action :name "create-function")
(gen-code-action :name "get-in-all")
(gen-code-action :name "get-in-less")
(gen-code-action :name "get-in-more")
(gen-code-action :name "get-in-none")
(gen-code-action :name "introduce-let")
(gen-code-action :name "inline-symbol")
(gen-code-action :name "resolve-macro-as")
(gen-code-action :name "move-form")
(gen-code-action :name "move-to-let")
(gen-code-action :name "promote-fn")
(gen-code-action :name "replace-refer-all-with-refer")
(gen-code-action :name "replace-refer-all-with-alias")
(gen-code-action :name "restructure-keys")
(gen-code-action :name "change-coll")
(gen-code-action :name "sort-clauses")
(gen-code-action :name "thread-first-all")
(gen-code-action :name "thread-last-all")
(gen-code-action :name "unwind-all")
