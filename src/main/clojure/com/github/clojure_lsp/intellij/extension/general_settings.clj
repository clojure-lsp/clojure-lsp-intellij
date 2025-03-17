(ns com.github.clojure-lsp.intellij.extension.general-settings
  (:require
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.config :as config]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.server :as server]
   [com.github.clojure-lsp.intellij.settings :as settings]
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]]
   [seesaw.color :as s.color]
   [seesaw.core :as s]
   [seesaw.font :as s.font]
   [seesaw.mig :as s.mig])
  (:import
   [com.intellij.openapi.options Configurable]
   [com.intellij.ui IdeBorderFactory]
   [java.awt Toolkit]
   [java.awt.datatransfer StringSelection]))

(set! *warn-on-reflection* true)

(defonce ^:private component* (atom nil))

(def ^:private server-not-started-message "Server not started")

(defn ^:private build-component [{:keys [server-version log-path] :as server-info}]
  (let [server-running? (boolean server-info)
        custom-server-path (settings/server-path)
        server-path (or custom-server-path (.getCanonicalPath (config/download-server-path)))
        custom-server-log-path (settings/server-log-path)
        server-log-path (or custom-server-log-path log-path)]
    (s.mig/mig-panel
     :items (->> [(when-not server-running?
                    [(s/label :text "Warning: Clojure LSP is not running or not started yet"
                              :foreground (s.color/color 243 156 18)) "wrap"])
                  [(s.mig/mig-panel :border (IdeBorderFactory/createTitledBorder "Settings")
                                    :items [[(s/label "Server path *") ""]
                                            [(s/text :id :server-path
                                                     :columns 30
                                                     :editable? true
                                                     :enabled? custom-server-path
                                                     :text server-path) ""]
                                            [(s/checkbox :id :custom-server-path?
                                                         :selected? custom-server-path
                                                         :text "Custom path?"
                                                         :listen [:action (fn [_]
                                                                            (let [enabled? (s/config (s/select @component* [:#custom-server-path?]) :selected?)
                                                                                  server-path-component (s/select @component* [:#server-path])]
                                                                              (s/config! server-path-component :text "")
                                                                              (s/config! server-path-component :enabled? enabled?)))]) "wrap"]]) "span"]
                  [(s/label :text "When not speciying a custom server path, the plugin will download the latest clojure-lsp automatically."
                            :font (s.font/font  :size 14)
                            :foreground (s.color/color 110 110 110)) "wrap"]
                  [(s.mig/mig-panel :border (IdeBorderFactory/createTitledBorder "Troubleshooting")
                                    :items [[(s/label "Server log path *") ""]
                                            [(s/text :id :server-log
                                                     :columns 30
                                                     :editable? true
                                                     :enabled? custom-server-log-path
                                                     :text server-log-path) ""]
                                            [(s/checkbox :id :custom-server-log?
                                                         :selected? custom-server-log-path
                                                         :text "Custom path?"
                                                         :listen [:action (fn [_]
                                                                            (let [enabled? (s/config (s/select @component* [:#custom-server-log?]) :selected?)
                                                                                  server-log-component (s/select @component* [:#server-log])]
                                                                              (s/config! server-log-component :text "")
                                                                              (s/config! server-log-component :enabled? enabled?)))]) "wrap"]
                                            [(s/label "Server version") ""]
                                            [(s/text :id :server-version
                                                     :columns 20
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
                                                                          (doseq [project (db/all-projects)]
                                                                            (server/shutdown! project)
                                                                            (server/start! project)))]) "wrap"]]) "span"]
                  [(s/label :text "*  requires LSP restart"
                            :font (s.font/font  :size 14)
                            :foreground (s.color/color 110 110 110)) "wrap"]]
                 (remove nil?)))))

(def-extension ClojureGeneralSettingsConfigurable []
  Configurable
  (createComponent [_]
    (let [project (first (db/all-projects))
          server-info (lsp-client/server-info project)
          component (build-component server-info)]
      (reset! component* component)
      component))

  (getPreferredFocusedComponent [_]
    (s/select @component* [:#copy-server-info]))

  (isModified [_]
    (let [server-path (s/config (s/select @component* [:#server-path]) :text)
          trace-level-combo-box (s/config (s/select @component* [:#trace-level]) :selected-item)
          server-log-path (s/config (s/select @component* [:#server-log]) :text)]
      (boolean
       (or (not= server-path (or (settings/server-path) ""))
           (not= trace-level-combo-box (settings/server-trace-level))
           (not= server-log-path (or (settings/server-log-path) ""))
           (and (str/blank? server-log-path)
                (settings/server-log-path))))))

  (reset [_]
    (let [project (first (db/all-projects))
          server-info (lsp-client/server-info project)
          trace-level-combo-box (settings/server-trace-level)
          server-log-path (or (settings/server-log-path) (:log-path server-info))
          server-path (or (settings/server-path) (.getCanonicalPath (config/download-server-path)))]
      (s/config! (s/select @component* [:#trace-level]) :selected-item trace-level-combo-box)
      (s/config! (s/select @component* [:#server-log]) :text server-log-path)
      (s/config! (s/select @component* [:#server-path]) :text server-path)))

  (disposeUIResources [_]
    (reset! component* nil))

  (apply [_]
    (let [trace-level (s/config (s/select @component* [:#trace-level]) :selected-item)
          server-log-path (when (s/config (s/select @component* [:#custom-server-log?]) :selected?)
                            (s/config (s/select @component* [:#server-log]) :text))
          server-path (when (s/config (s/select @component* [:#custom-server-path?]) :selected?)
                        (s/config (s/select @component* [:#server-path]) :text))]
      (settings/set-server-path! server-path)
      (settings/set-server-log-path! server-log-path)
      (settings/set-server-trace-level! trace-level)))

  (cancel [_]))
