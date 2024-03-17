(ns com.github.clojure-lsp.intellij.db
  (:require
   [clojure.core.async :as async])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]))

(set! *warn-on-reflection* true)

(def ^:private initial-db
  {:status :disconnected
   :downloaded-server-path nil
   :on-status-changed-fns []
   :client nil
   :server-process nil
   :project nil
   :diagnostics {}
   :settings {:trace-level "off"
              :server-path nil}})

(defonce db* (atom initial-db))

(defmacro await-init [field & body]
  `(async/go-loop [~'value (get @db* ~field)]
     (Thread/sleep 50)
     (if ~'value
       ~@body
       (recur (get @db* ~field)))))

(defn load-settings-from-state! [^SettingsState settings-state]
  (swap! db* update :settings (fn [settings]
                                (if-not (:loaded-settings? settings)
                                  (-> settings
                                      (assoc :loaded-settings? true)
                                      (update :server-path #(or (.getServerPath settings-state) %))
                                      (update :trace-level #(or (.getTraceLevel settings-state) %))
                                      (update :log-path #(or (.getServerLogPath settings-state) %)))
                                  settings))))

(defn set-trace-level-setting! [^SettingsState settings-state trace-level]
  (.setTraceLevel settings-state trace-level)
  (swap! db* assoc-in [:settings :trace-level] trace-level))

(defn set-server-log-path-setting! [^SettingsState settings-state log-path]
  (let [log-path (not-empty log-path)]
    (.setServerLogPath settings-state log-path)
    (swap! db* assoc-in [:settings :log-path] log-path)))

(defn set-server-path-setting! [^SettingsState settings-state server-path]
  (let [server-path (not-empty server-path)]
    (.setServerPath settings-state server-path)
    (swap! db* assoc-in [:settings :server-path] server-path)))
