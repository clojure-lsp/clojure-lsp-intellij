(ns com.github.clojure-lsp.intellij.db
  (:require
   [clojure.core.async :as async])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]))

(set! *warn-on-reflection* true)

(def ^:private initial-db
  {:status :disconnected
   :on-status-changed-fns []
   :client nil
   :server nil
   :project nil
   :diagnostics {}
   :settings {:trace-level "off"}})

(defonce db* (atom initial-db))

(defmacro await-init [field & body]
  `(async/go-loop [~'value (get @db* ~field)]
     (if ~'value
       ~@body
       (recur (get @db* ~field)))))

(defn load-settings-from-state! [^SettingsState settings-state]
  (swap! db* update :settings (fn [settings]
                                (-> settings
                                    (update :trace-level #(or (.getTraceLevel settings-state) %))))))

(defn set-trace-level-setting! [^SettingsState settings-state trace-level]
  (.setTraceLevel settings-state trace-level)
  (swap! db* assoc-in [:settings :trace-level] trace-level))
