(ns com.github.clojure-lsp.intellij.extension.status-bar-factory
  (:gen-class
   :post-init post-init
   :name com.github.clojure_lsp.intellij.extension.StatusBarFactory
   :implements [com.intellij.openapi.wm.StatusBarWidgetFactory])
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.server :as server]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.openapi.actionSystem AnAction DefaultActionGroup]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.ui.popup JBPopupFactory JBPopupFactory$ActionSelectionAid]
   [com.intellij.openapi.vfs VirtualFile]
   [com.intellij.openapi.wm StatusBarWidget]
   [com.intellij.openapi.wm.impl.status EditorBasedStatusBarPopup EditorBasedStatusBarPopup$WidgetState]))

(set! *warn-on-reflection* true)

(def ^:const widget-id "ClojureLSPStatusBar")

(defn -getId [_] widget-id)

(defn -getDisplayName [_] "Clojure LSP")

(defn -isAvailable [_ _] true)

(defn -canBeEnabledOn [_ _] true)

(defn -isConfigurable [_] false)

(defn -isEnabledByDefault [_] true)

(defn -disposeWidget [_ _])

(defn ^:private refresh-status-bar [^Project _project]
  ;; TODO Not properly refreshing status bar
  #_(let [manager-service ^StatusBarWidgetsManager (.getService project StatusBarWidgetsManager)
          settings-service ^StatusBarWidgetSettings (.getService project StatusBarWidgetSettings)
          factory ^StatusBarWidgetFactory (.findWidgetFactory manager-service widget-id)]
      (.setEnabled settings-service factory false)
      (.updateWidget manager-service factory)))

(defn ^:private restart-lsp-action [^Project project]
  (proxy+
   ["Restart server"]
    AnAction
    (update [_ _event])

    (actionPerformed [_ _event]
                     (server/spawn-server! project))))

(defn -post-init [_this]
  (swap! db/db* update :on-status-changed-fns conj
         (fn [_status]
           (refresh-status-bar (:project @db/db*)))))

(defn -createWidget ^StatusBarWidget [_this ^Project project]
  (proxy+
   [project false]
    EditorBasedStatusBarPopup
    (ID [_this] widget-id)
    (dispose [_this]
             #_(reset! current-status-bar* nil))
    (getWidgetState [_this ^VirtualFile _file]
                    (let [_icon ^Icon (if (identical? :connected (:status @db/db*))
                                        (Icons/STATUS_CONNECTED)
                                        (Icons/STATUS_DISCONNECTED))
                          widget-state ^EditorBasedStatusBarPopup$WidgetState (proxy+ ["Clojure LSP actions" "LSP" true] EditorBasedStatusBarPopup$WidgetState)]
                      ;; TODO check how add icon
                      ;; (.setIcon widget-state icon)
                      widget-state))

    (createPopup [_this context]
                 (let [action-group (doto (DefaultActionGroup.)
                                      (.add (restart-lsp-action project)))]
                   (.createActionGroupPopup
                    (JBPopupFactory/getInstance)
                    (str "Clojure LSP: " (name (:status @db/db*)))
                    action-group
                    context
                    (JBPopupFactory$ActionSelectionAid/SPEEDSEARCH)
                    true)))))
