(ns com.github.clojure-lsp.intellij.extension.general-settings
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.GeneralSettingsConfigurable
   :implements [com.intellij.openapi.options.Configurable])
  (:require
   [clojure.pprint :as pprint]
   [clojure.walk :as walk]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.server :as server]
   [seesaw.core :as see]
   [seesaw.mig :as mig])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]
   [com.intellij.ui IdeBorderFactory]
   [java.awt Toolkit]
   [java.awt.datatransfer StringSelection]
   [javax.swing JComboBox]))

(set! *warn-on-reflection* true)

(defonce ^:private component* (atom nil))

(def ^:private server-not-started-message "Server not started")

(defn ^:private build-component [{:keys [server-version log-path] :as server-info}]
  (mig/mig-panel
   :items [[(mig/mig-panel :border (IdeBorderFactory/createTitledBorder "Troubleshooting")
                           :items [[(see/label "Server log path") ""]
                                   [(see/text :id :server-log
                                              :columns 30
                                              :editable? false
                                              :text (or log-path server-not-started-message)) "wrap"]
                                   [(see/label "Server version") ""]
                                   [(see/text :id :server-log
                                              :columns 30
                                              :editable? false
                                              :text (or server-version server-not-started-message)) "wrap"]
                                   [(see/label "Server trace level (Requires LSP restart)") ""]
                                   [(see/combobox :id :trace-level :model ["off" "messages" "verbose"]) "wrap"]
                                   [(see/button :text "Copy server info to clipboard"
                                                :listen [:action (fn [e]
                                                                   (.setContents (.getSystemClipboard (Toolkit/getDefaultToolkit))
                                                                                 (StringSelection. (with-out-str (pprint/pprint server-info)))
                                                                                 nil)
                                                                   (see/alert e "Server info copied to clipboard"))]) ""]]) "span"]]))

(defn -createComponent [_]
  (let [server-info (when-let [client (server/connected-client)]
                      (some-> (lsp-client/request! client [":clojure/serverInfo/raw" {}])
                              deref
                              walk/keywordize-keys))
        component (build-component server-info)]
    (reset! component* component)
    component))

(defn -getPreferredFocusedComponent [_]
  (see/select @component* [:#trace-level]))

(defn -isModified [_]
  (let [settings-state (SettingsState/get)
        trace-level-combo-box ^JComboBox (see/select @component* [:#trace-level])]
    (not= (.getSelectedItem trace-level-combo-box) (.getTraceLevel settings-state))))

(defn -reset [_]
  (let [trace-level-combo-box ^JComboBox (see/select @component* [:#trace-level])]
    (.setSelectedItem trace-level-combo-box (-> @db/db* :settings :trace-level))))

(defn -disposeUIResources [_]
  (reset! component* nil))

(defn -apply [_]
  (let [settings-state (SettingsState/get)
        trace-level-combo-box ^JComboBox (see/select @component* [:#trace-level])]
    (db/set-trace-level-setting! settings-state (.getSelectedItem trace-level-combo-box))))

(defn -cancel [_])
