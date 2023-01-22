(ns com.github.clojure-lsp.intellij.extension.status-bar-factory
  (:gen-class
   :post-init post-init
   :name com.github.clojure_lsp.intellij.extension.StatusBarFactory
   :extends com.github.clojure_lsp.intellij.WithLoader
   :implements [com.intellij.openapi.wm.StatusBarWidgetFactory])
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.server :as server])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.openapi.actionSystem AnAction DefaultActionGroup]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.ui.popup JBPopupFactory JBPopupFactory$ActionSelectionAid]
   [com.intellij.openapi.vfs VirtualFile]
   [com.intellij.openapi.wm StatusBarWidget]
   [com.intellij.openapi.wm.impl.status EditorBasedStatusBarPopup EditorBasedStatusBarPopup$WidgetState]
   [com.intellij.openapi.wm.impl.status.widget StatusBarWidgetsManager]
   [javax.swing Icon]))

(set! *warn-on-reflection* true)

(def ^:const widget-id "clojure-lsp-status-bar")

(defn -getId [_] widget-id)

(defn -getDisplayName [_] "Clojure LSP")

(defn -isAvailable [_ _] true)

(defn -canBeEnabledOn [_ _] true)

(defn -isConfigurable [_] false)

(defn -isEnabledByDefault [_] true)

(defn -disposeWidget [_ _])

#_(def current-status-bar* (atom nil))

(defn ^:private refresh-status-bar [^Project project]
  ;; TODO improve update only lsp widget
  (let [service ^StatusBarWidgetsManager (.getService project StatusBarWidgetsManager)]
    (.disableAllWidgets service)
    (.updateAllWidgets service)))

(defn ^:private restart-lsp-action [^Project project]
  (proxy [AnAction] ["Restart server"]
    (update [event])

    (actionPerformed [event]
      (server/spawn-server! project))))

(defn -post-init [_this]
  (swap! db/db* update :on-status-changed-fns conj
         (fn [_status]
           (refresh-status-bar (:project @db/db*)))))

(defn -createWidget ^StatusBarWidget [_this ^Project project]
  (proxy [EditorBasedStatusBarPopup] [project false]
    (ID [] widget-id)
    (dispose []
      #_(reset! current-status-bar* nil))
    (getWidgetState [^VirtualFile file]
      (let [icon ^Icon (if (identical? :connected (:status @db/db*))
                         (Icons/StatusConnected)
                         (Icons/StatusDisconnected))
            widget-state (proxy [EditorBasedStatusBarPopup$WidgetState] ["Clojure LSP actions" nil true])]
        (.setIcon widget-state icon)
        widget-state))

    (createPopup [context]
      (let [action-group (doto (DefaultActionGroup.)
                           (.add (restart-lsp-action project)))]
        (.createActionGroupPopup
         (JBPopupFactory/getInstance)
         (str "Clojure LSP: " (name (:status @db/db*)))
         action-group
         context
         (JBPopupFactory$ActionSelectionAid/SPEEDSEARCH)
         true)))

    #_(^void install [^StatusBar status-bar]
                     (reset! current-status-bar* status-bar))))
