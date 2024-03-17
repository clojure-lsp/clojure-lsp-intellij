(ns com.github.clojure-lsp.intellij.action.references
  (:gen-class
   :name com.github.clojure_lsp.intellij.action.ReferencesAction
   :extends com.intellij.openapi.project.DumbAwareAction)
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.editor :as editor]
   [com.github.clojure-lsp.intellij.psi :as psi]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [seesaw.core :as s]
   [seesaw.mig :as s.mig])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.github.clojure_lsp.intellij ClojureFileType]
   [com.intellij.codeInsight.hint HintManager]
   [com.intellij.codeInsight.navigation NavigationUtil]
   [com.intellij.ide IdeEventQueue]
   [com.intellij.openapi.actionSystem
    CommonDataKeys]
   [com.intellij.openapi.actionSystem AnActionEvent]
   [com.intellij.openapi.editor Editor EditorFactory]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.ui.popup JBPopupFactory]
   [com.intellij.ui EditorTextField]
   [com.intellij.ui.components JBList]
   [com.intellij.util NotNullFunction]
   [com.intellij.util.ui JBFont JBUI$CurrentTheme$List]
   [java.awt Cursor]
   [java.awt.event KeyEvent]))

(set! *warn-on-reflection* true)

(defn ^:private reference->psi-element [{:keys [uri] {:keys [start end]} :range} ^Project project]
  (let [text (slurp uri)
        start-offset (editor/position->offset text (:line start) (:character start))
        end-offset (editor/position->offset text (:line end) (:character end))
        file (editor/uri->psi-file uri project)
        name (subs text start-offset end-offset)]
    (psi/->LSPPsiElement name project file start-offset end-offset (:line start))))

(defn get-references [^Editor editor line character]
  (when-let [client (:client @db/db*)]
    @(lsp-client/request! client [:textDocument/references
                                  {:text-document {:uri (editor/editor->uri editor)}
                                   :position {:line line
                                              :character character}}])))

(defn get-psi-references [^Editor editor line character]
  (let [project ^Project (.getProject editor)]
    (->> (get-references editor line character)
         (mapv #(reference->psi-element % project)))))

(defn ^:private references-popup [references ^Editor editor]
  (let [project (.getProject editor)
        list (JBList. ^java.util.Collection references)
        go-to-reference-fn (fn [_]
                             (let [reference (s/selection list)]
                               (NavigationUtil/activateFileWithPsiElement (reference->psi-element reference project) true)
                               (.closeAllPopups (.getPopupManager (IdeEventQueue/getInstance)))))]
    (.installCellRenderer list (reify NotNullFunction
                                 (fun [_ {:keys [uri] {:keys [start]} :range}]
                                   (let [text (slurp uri)
                                         code ^String (editor/code-at-line text (:line start))
                                         document (.createDocument (EditorFactory/getInstance) code)
                                         filename-field (s/label :id :reference-icon
                                                                 :icon Icons/CLOJURE
                                                                 :text (editor/uri->project-relative-filename uri project))
                                         line-field (s/label :id :reference-file
                                                             :text (:line start)
                                                             :font (.deriveFont (JBFont/label) java.awt.Font/BOLD))
                                         code-field (doto (EditorTextField. document project (ClojureFileType/INSTANCE) true false)
                                                      (.setFocusTraversalPolicyProvider false)
                                                      (.setFocusable false)
                                                      (.ensureWillComputePreferredSize)
                                                      (s/config! :background (JBUI$CurrentTheme$List/background false false)))
                                         item (s.mig/mig-panel
                                               :constraints ["ins 2 10 2 10" "[][][grow]"]
                                               :items [[filename-field ""]
                                                       [line-field "push"]
                                                       [code-field "right"]])]
                                     (s/listen item :mouse-entered (fn [_] (s/config! item :cursor (Cursor. Cursor/HAND_CURSOR))))
                                     (s/listen item :mouse-exited (fn [_] (s/config! item :cursor (Cursor. Cursor/HAND_CURSOR))))
                                     item))))
    (.setSelectedIndex list 0)
    (s/listen list :mouse-clicked go-to-reference-fn)
    (s/listen list :key-pressed (fn [^KeyEvent e]
                                  (when (= (.getKeyCode e) KeyEvent/VK_ENTER)
                                    (go-to-reference-fn e))))

    list))

(defn show-references [^Editor editor line character]
  (when-let [references (get-references editor line character)]
    (if (= 1 (count references))
      (NavigationUtil/activateFileWithPsiElement (reference->psi-element (first references) (.getProject editor)) true)
      (app-manager/write-action!
       {:run-fn (fn []
                  (if (seq references)
                    (-> (JBPopupFactory/getInstance)
                        (.createComponentPopupBuilder (references-popup references editor) nil)
                        (.setTitle (str (count references) " References"))
                        (.setResizable false)
                        (.setRequestFocus true)
                        (.createPopup)
                        (.showInBestPositionFor editor))
                    (.showErrorHint (HintManager/getInstance)
                                    editor
                                    "No references found")))}))))

(defn -actionPerformed [_ ^AnActionEvent event]
  (when-let [editor ^Editor (.getData event CommonDataKeys/EDITOR)]
    (let [[line character] (editor/editor->cursor-position editor)]
      (show-references editor line character))))
