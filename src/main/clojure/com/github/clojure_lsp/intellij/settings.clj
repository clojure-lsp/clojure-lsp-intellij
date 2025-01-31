(ns com.github.clojure-lsp.intellij.settings
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]))

(set! *warn-on-reflection* true)

(defn server-path []
  (.getServerPath (SettingsState/get)))

(defn server-trace-level []
  (or (.getTraceLevel (SettingsState/get)) "off"))

(defn server-log-path []
  (.getServerLogPath (SettingsState/get)))

(defn set-server-trace-level! [^String trace-level]
  (.setTraceLevel (SettingsState/get) trace-level))

(defn set-server-log-path! [^String log-path]
  (.setServerLogPath (SettingsState/get) (not-empty log-path)))

(defn set-server-path! [^String server-path]
  (.setServerPath (SettingsState/get) (not-empty server-path)))
