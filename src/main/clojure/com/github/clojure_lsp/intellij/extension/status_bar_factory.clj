(ns com.github.clojure-lsp.intellij.extension.status-bar-factory
  (:gen-class
   :post-init post-init
   :name com.github.clojure_lsp.intellij.extension.StatusBarFactory
   :implements [com.intellij.openapi.wm.StatusBarWidgetFactory])
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.project-lsp :as project]
   [com.github.clojure-lsp.intellij.server :as server]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.openapi.actionSystem AnAction DataContext DefaultActionGroup]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.ui.popup JBPopupFactory JBPopupFactory$ActionSelectionAid]
   [com.intellij.openapi.wm
    StatusBarWidget
    StatusBarWidget$IconPresentation
    StatusBarWidgetFactory
    WindowManager]
   [com.intellij.openapi.wm.impl.status.widget StatusBarWidgetsManager]
   [com.intellij.ui.awt RelativePoint]
   [com.intellij.util Consumer]
   [java.awt Point]
   [java.awt.event MouseEvent]))

(set! *warn-on-reflection* true)

(def ^:const widget-id "ClojureLSPStatusBar")

(defn -getId [_] widget-id)

(defn -getDisplayName [_] "Clojure LSP")

(defn -isAvailable [_ project]
  (project/clojure-project? project @db/db*))

(defn -canBeEnabledOn [_ _] true)

(defn -isConfigurable [_] true)

(defn -isEnabledByDefault [_] true)

(defn -disposeWidget [_ _])

(defn ^:private refresh-status-bar [^StatusBarWidgetFactory factory ^Project project]
  (when-let [status-bar (.getStatusBar (WindowManager/getInstance) project)]
    (.updateWidget status-bar widget-id)
    (.updateWidget ^StatusBarWidgetsManager (.getService project StatusBarWidgetsManager) factory)))

(defn -post-init [this]
  (swap! db/db* update :on-status-changed-fns conj
         (fn [_status]
           (refresh-status-bar this (:project @db/db*)))))

(defn ^:private restart-lsp-action [^Project project]
  (proxy+
   ["Restart server"]
   AnAction
    (update [_ _event])

    (actionPerformed [_ _event]
      (server/shutdown!)
      (server/spawn-server! project))))

(defn ^:private status-bar-title []
  (str "Clojure LSP: " (name (:status @db/db*))))

(defn -createWidget ^StatusBarWidget [_this ^Project project]
  (proxy+
   []
   StatusBarWidget
    (ID [_] widget-id)
    (getPresentation [this] this)
    StatusBarWidget$IconPresentation
    (getClickConsumer [_]
      (reify Consumer
        (consume [_ e]
          (let [popup (.createActionGroupPopup
                       (JBPopupFactory/getInstance)
                       (status-bar-title)
                       (doto (DefaultActionGroup.)
                         (.add (restart-lsp-action project)))
                       DataContext/EMPTY_CONTEXT
                       JBPopupFactory$ActionSelectionAid/SPEEDSEARCH
                       true)]
            (.show popup (RelativePoint. (.getComponent ^MouseEvent e) (Point. 0 (-> popup .getContent .getPreferredSize .getHeight -)))))
          true)))
    (getTooltipText [_] (status-bar-title))
    (getIcon [_]
      (if (lsp-client/connected-client)
        Icons/STATUS_CONNECTED
        Icons/STATUS_DISCONNECTED))))
