(ns com.github.clojure-lsp.intellij.extension.status-bar-factory
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.project-lsp :as project]
   [com.github.clojure-lsp.intellij.server :as server]
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.ide DataManager]
   [com.intellij.openapi.actionSystem DefaultActionGroup]
   [com.intellij.openapi.project DumbAwareAction Project]
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
   [java.awt.event MouseEvent]
   [kotlinx.coroutines CoroutineScope]))

(set! *warn-on-reflection* true)

(def ^:const widget-id "ClojureLSPStatusBar")

(defn ^:private refresh-status-bar [^StatusBarWidgetFactory factory ^Project project]
  (when-let [status-bar (.getStatusBar (WindowManager/getInstance) project)]
    (.updateWidget status-bar widget-id)
    (.updateWidget ^StatusBarWidgetsManager (.getService project StatusBarWidgetsManager) factory)))

(defn ^:private restart-lsp-action [^Project project]
  (proxy+
   ["Restart server"]
   DumbAwareAction
    (actionPerformed [_ _event]
      (server/shutdown! project)
      (server/start! project))))

(defn ^:private status-bar-title [project]
  (str "Clojure LSP: " (name (lsp-client/server-status project))))

(def-extension ClojureStatusBarFactory []
  StatusBarWidgetFactory
  (getId [_] widget-id)

  (getDisplayName [_] "Clojure LSP")

  (isAvailable [_ project]
    (project/clojure-project? project))

  (canBeEnabledOn [_ _] true)

  (isConfigurable [_] true)

  (isEnabledByDefault [_] true)

  (disposeWidget [_ _])

  (createWidget ^StatusBarWidget
    ([^StatusBarWidgetFactory this ^Project project ^CoroutineScope _]
     (.createWidget this project))
    ([this ^Project project]
     (db/update-in project [:on-status-changed-fns] #(conj % (fn [_status]
                                                               (refresh-status-bar this project))))
     (proxy+
      []
      StatusBarWidget
       (ID [_] widget-id)
       (dispose [_])
       (install [_ _])
       (getPresentation [this] this)
       StatusBarWidget$IconPresentation
       (getClickConsumer [_]
         (reify Consumer
           (consume [_ e]
             (let [component (.getComponent ^MouseEvent e)
                   popup (.createActionGroupPopup
                          (JBPopupFactory/getInstance)
                          (status-bar-title project)
                          (doto (DefaultActionGroup.)
                            (.add (restart-lsp-action project)))
                          (.getDataContext (DataManager/getInstance) component)
                          JBPopupFactory$ActionSelectionAid/SPEEDSEARCH
                          true)]
               (.show popup (RelativePoint. component (Point. 0 (-> popup .getContent .getPreferredSize .getHeight -)))))
             true)))
       (getTooltipText [_] (status-bar-title project))
       (getIcon [_]
         (if (= :started (lsp-client/server-status project))
           Icons/STATUS_CONNECTED
           Icons/STATUS_DISCONNECTED))))))
