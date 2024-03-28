(ns com.github.clojure-lsp.intellij.db
  (:refer-clojure :exclude [get-in assoc-in update-in])
  (:require
   [clojure.core.async :as async])
  (:import
   [com.github.clojure_lsp.intellij.extension SettingsState]
   [com.intellij.openapi.project Project]))

(set! *warn-on-reflection* true)

(def ^:private empty-project
  {:status :disconnected
   :downloaded-server-path nil
   :client nil
   :server-process nil
   :project nil
   :diagnostics {}
   :settings {:trace-level "off"
              :server-path nil}})

(defonce db* (atom {:projects {}
                    :on-status-changed-fns []}))

(defn empty-db? []
  (empty? (:projects @db*)))

(defn get-in
  ([project fields]
   (get-in project fields nil))
  ([^Project project fields default]
   (clojure.core/get-in @db* (concat [:projects (.getBasePath project)] fields) default)))

(defn assoc-in [^Project project fields value]
  (swap! db* clojure.core/assoc-in (concat [:projects (.getBasePath project)] fields) value))

(defn update-in [^Project project fields fn]
  (swap! db* clojure.core/update-in (concat [:projects (.getBasePath project)] fields) fn))

(defn init-db-for-project [^Project project]
  (swap! db* update :projects (fn [projects]
                                (if (clojure.core/get projects (.getBasePath project))
                                  projects
                                  (assoc projects (.getBasePath project) (assoc empty-project :project project))))))

(defn await-field [project field fn]
  (async/go-loop []
    (Thread/sleep 100)
    (let [value (get-in project [field])]
      (if value
        (fn value)
        (recur)))))

(defn load-settings-from-state! [^Project project ^SettingsState settings-state]
  (update-in project [:settings] (fn [settings]
                                   (if-not (:loaded-settings? settings)
                                     (-> settings
                                         (assoc :loaded-settings? true)
                                         (update :server-path #(or (.getServerPath settings-state) %))
                                         (update :trace-level #(or (.getTraceLevel settings-state) %))
                                         (update :log-path #(or (.getServerLogPath settings-state) %)))
                                     settings))))

(defn set-trace-level-setting! [^SettingsState settings-state trace-level]
  (doseq [project-path (keys (:projects @db*))]
    (.setTraceLevel settings-state trace-level)
    (swap! db* clojure.core/assoc-in [:projects project-path :settings :trace-level] trace-level)))

(defn set-server-log-path-setting! [^SettingsState settings-state log-path]
  (doseq [project-path (keys (:projects @db*))]
    (let [log-path (not-empty log-path)]
      (.setServerLogPath settings-state log-path)
      (swap! db* clojure.core/assoc-in [:projects project-path :settings :log-path] log-path))))

(defn set-server-path-setting! [^SettingsState settings-state server-path]
  (doseq [project-path (keys (:projects @db*))]
    (let [server-path (not-empty server-path)]
      (.setServerPath settings-state server-path)
      (swap! db* clojure.core/assoc-in [:projects project-path :settings :server-path] server-path))))

(defn all-projects []
  (remove nil?
          (mapv :project (vals (:projects @db*)))))
