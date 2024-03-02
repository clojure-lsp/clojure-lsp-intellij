(ns com.github.clojure-lsp.intellij.extension.general-settings
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.GeneralSettingsConfigurable
   :implements [com.intellij.openapi.options.Configurable])
  (:require
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.server :as server]
   [seesaw.color :as s.color]
   [seesaw.core :as s]
   [seesaw.font :as s.font]
   [seesaw.mig :as s.mig])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]
   [com.intellij.ui IdeBorderFactory]
   [java.awt Toolkit]
   [java.awt.datatransfer StringSelection]
   [javax.swing JComboBox JTextField]))

(set! *warn-on-reflection* true)

(defonce ^:private component* (atom nil))

(def ^:private server-not-started-message "Server not started")

(defn ^:private build-component [{:keys [server-version log-path] :as server-info} settings]
  (let [server-running? (boolean server-info)]
    (s.mig/mig-panel
     :items (->> [(when-not server-running?
                    [(s/label :text "Warning: Clojure LSP is not running or not started yet"
                              :foreground (s.color/color 243 156 18)) "wrap"])
                  [(s.mig/mig-panel :border (IdeBorderFactory/createTitledBorder "Settings")
                                    :items [[(s/label "Custom server path *") ""]
                                            [(s/text :id :server-path
                                                     :columns 30
                                                     :editable? true
                                                     :enabled? true
                                                     :text (:server-path settings)) "wrap"]]) "span"]
                  [(s.mig/mig-panel :border (IdeBorderFactory/createTitledBorder "Troubleshooting")
                                    :items [[(s/label "Server log path *") ""]
                                            [(s/text :id :server-log
                                                     :columns 30
                                                     :editable? true
                                                     :enabled? true
                                                     :text log-path) "wrap"]
                                            [(s/label "Server version") ""]
                                            [(s/text :id :server-version
                                                     :columns 30
                                                     :editable? false
                                                     :enabled? server-running?
                                                     :text (or server-version server-not-started-message)) "wrap"]
                                            [(s/label "Server trace level *") ""]
                                            [(s/combobox :id :trace-level :model ["off" "messages" "verbose"]) "wrap"]
                                            [(s/button :id :copy-server-info
                                                       :text "Copy server info to clipboard"
                                                       :listen [:action (fn [e]
                                                                          (.setContents (.getSystemClipboard (Toolkit/getDefaultToolkit))
                                                                                        (StringSelection. (with-out-str (pprint/pprint server-info)))
                                                                                        nil)
                                                                          (s/alert e "Server info copied to clipboard"))]) "wrap"]
                                            [(s/button :text "Restart LSP server"
                                                       :listen [:action (fn [_]
                                                                          (server/shutdown!)
                                                                          (server/start-server! (:project @db/db*)))]) "wrap"]]) "span"]
                  [(s/label :text "*  requires LSP restart"
                            :font (s.font/font  :size 14)
                            :foreground (s.color/color 110 110 110)) "wrap"]]
                 (remove nil?)))))

(defn ^:private server-info! []
  (some-> (lsp-client/connected-client)
          (lsp-client/request! [":clojure/serverInfo/raw" {}])
          deref
          walk/keywordize-keys))

(defn -createComponent [_]
  (let [server-info (server-info!)
        component (build-component server-info (:settings @db/db*))]
    (reset! component* component)
    component))

(defn -getPreferredFocusedComponent [_]
  (s/select @component* [:#copy-server-info]))

(defn -isModified [_]
  (let [settings-state (SettingsState/get)
        server-path ^JTextField (s/select @component* [:#server-path])
        trace-level-combo-box ^JComboBox (s/select @component* [:#trace-level])
        server-log-path ^JTextField (s/select @component* [:#server-log])]
    (boolean
     (or (not= (.getText server-path) (or (.getServerPath settings-state) ""))
         (not= (.getSelectedItem trace-level-combo-box) (.getTraceLevel settings-state))
         (not= (.getText server-log-path) (or (.getServerLogPath settings-state) ""))
         (and (str/blank? (.getText server-log-path))
              (-> @db/db* :settings :log-path))))))

(defn -reset [_]
  (let [trace-level-combo-box ^JComboBox (s/select @component* [:#trace-level])
        server-log-path ^JTextField (s/select @component* [:#server-log])
        server-path ^JTextField (s/select @component* [:#server-path])
        server-info (server-info!)]
    (.setSelectedItem trace-level-combo-box (-> @db/db* :settings :trace-level))
    (.setText server-log-path (or (-> @db/db* :settings :log-path) (:log-path server-info)))
    (.setText server-path (or (-> @db/db* :settings :server-path) ""))))

(defn -disposeUIResources [_]
  (reset! component* nil))

(defn -apply [_]
  (let [settings-state (SettingsState/get)
        trace-level (.getSelectedItem ^JComboBox (s/select @component* [:#trace-level]))
        server-log-path (.getText ^JTextField (s/select @component* [:#server-log]))
        server-path (.getText ^JTextField (s/select @component* [:#server-path]))]
    (db/set-server-path-setting! settings-state server-path)
    (db/set-server-log-path-setting! settings-state server-log-path)
    (db/set-trace-level-setting! settings-state trace-level)))

(defn -cancel [_])
