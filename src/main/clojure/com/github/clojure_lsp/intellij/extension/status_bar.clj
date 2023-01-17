(ns com.github.clojure-lsp.intellij.extension.status-bar
  (:gen-class
   :post-init post-init
   :name com.github.clojure_lsp.intellij.extension.StatusBar
   :extends com.github.clojure_lsp.intellij.WithLoader
   :implements [com.intellij.openapi.wm.StatusBarWidgetFactory])
  (:require
   [com.github.clojure-lsp.intellij.db :as db])
  (:import
   (com.github.clojure_lsp.intellij Icons)
   (com.intellij.openapi.project Project)
   (com.intellij.openapi.wm StatusBar StatusBarWidget StatusBarWidget$IconPresentation)))

(set! *warn-on-reflection* true)

(def ^:const widget-id "clojure-lsp-status-bar")

(defn -getId [_] widget-id)

(defn -getDisplayName [_] "Clojure LSP")

(defn -isAvailable [_ _] true)

(defn -canBeEnabledOn [_ _] true)

(defn -isConfigurable [_] false)

(defn -isEnabledByDefault [_] true)

(defn -disposeWidget [_ _])

(def current-status-bar* (atom nil))

(defn -post-init [_this]
  (swap! db/db* update :on-status-changed-fns conj
         (fn [_status]
           (when-let [status-bar ^StatusBar @current-status-bar*]
             (.updateWidget status-bar widget-id)))))

(defn -createWidget ^StatusBarWidget [_this ^Project _project]
  (reify
    StatusBarWidget
    (ID [_] widget-id)
    (dispose [_]
      (reset! current-status-bar* nil))
    (^void install [_ ^StatusBar status-bar]
      (reset! current-status-bar* status-bar))
    (getPresentation [this] this)

    StatusBarWidget$IconPresentation
    (getIcon [_]
      (if (identical? :connected (:status @db/db*))
        (Icons/StatusConnected)
        (Icons/StatusDisconnected)))
    (getClickConsumer [_])
    (getTooltipText [_]
      "Clojure LSP actions")))
