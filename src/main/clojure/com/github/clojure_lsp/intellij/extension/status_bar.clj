(ns com.github.clojure-lsp.intellij.extension.status-bar
  (:import
   [com.github.clojure_lsp.intellij Icons]
   (com.intellij.openapi.project Project)
   (com.intellij.openapi.wm StatusBarWidget StatusBarWidget$IconPresentation))
  (:gen-class
   :main false
   :name com.github.clojure_lsp.intellij.extension.StatusBar
   :extends com.github.clojure_lsp.intellij.WithLoader
   :implements [com.intellij.openapi.wm.StatusBarWidgetFactory]))

(set! *warn-on-reflection* true)

(def ^:const widget-id "clojure-lsp-status-bar")

(defn -getId [_] widget-id)

(defn -getDisplayName [_] "Clojure LSP")

(defn -isAvailable [_ _] true)

(defn -canBeEnabledOn [_ _] true)

(defn -isConfigurable [_] false)

(defn -createWidget ^StatusBarWidget [_this ^Project project]
  (reify
    StatusBarWidget
    (ID [_] widget-id)
    (install [_ _])
    (getPresentation [this] this)

    StatusBarWidget$IconPresentation
    (getIcon [_]
      (Icons/StatusBar))
    (getClickConsumer [_])
    (getTooltipText [_]
      "Clojure LSP actions")))
