(ns com.github.clojure-lsp.intellij.extension.general-settings
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.GeneralSettingsConfigurable
   :implements [com.intellij.openapi.options.Configurable])
  (:require
   [com.github.clojure-lsp.intellij.db :as db])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]
   [com.intellij.util.ui FormBuilder]
   [java.awt Component FlowLayout]
   [javax.swing JComboBox JLabel JPanel]))

(set! *warn-on-reflection* true)

(defonce ^:private component* (atom nil))

(def ^:private trace-level-id "trace-level")
(def ^:private settings {trace-level-id ["off" "messages" "verbose"]})

(defn ^:private build-component []
  (let [trace-combo-box (JComboBox. ^"[Ljava.lang.String;" (into-array String (get settings trace-level-id)))
        trace-panel (doto (JPanel. (FlowLayout. FlowLayout/LEFT))
                      (.setAlignmentX Component/LEFT_ALIGNMENT)
                      (.add (JLabel. "Server trace level"))
                      (.add trace-combo-box))
        panel (.getPanel
               (doto (FormBuilder/createFormBuilder)
                 (.addComponent trace-panel)
                 (.addComponentFillVertically (JPanel.) 2)))]
    (.putClientProperty panel trace-level-id trace-combo-box)
    panel))

(defn -createComponent [_]
  (let [component (build-component)]
    (reset! component* component)
    component))

(defn -getPreferredFocusedComponent [_]
  (.getClientProperty ^JLabel @component* trace-level-id))

(defn -isModified [_]
  (let [settings-state (SettingsState/get)
        component ^JLabel @component*
        trace-level-combo-box ^JComboBox (.getClientProperty component trace-level-id)]
    (not= (.getSelectedItem trace-level-combo-box) (.getTraceLevel settings-state))))

(defn -reset [_]
  (let [component ^JLabel @component*
        trace-level-combo-box ^JComboBox (.getClientProperty component trace-level-id)]
    (.setSelectedItem trace-level-combo-box (-> @db/db* :settings :trace-level))))

(defn -disposeUIResources [_]
  (reset! component* nil))

(defn -apply [_]
  (let [settings-state (SettingsState/get)
        component ^JLabel @component*
        trace-level-combo-box ^JComboBox (.getClientProperty component trace-level-id)]
    (db/set-trace-level-setting! settings-state (.getSelectedItem trace-level-combo-box))))

(defn -cancel [_])
