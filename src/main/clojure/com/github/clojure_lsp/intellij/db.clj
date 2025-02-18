(ns com.github.clojure-lsp.intellij.db
  (:refer-clojure :exclude [get-in assoc-in update-in])
  (:import
   [com.intellij.openapi.project Project]))

(set! *warn-on-reflection* true)

(def ^:private empty-project
  {:status :disconnected
   :downloaded-server-path nil
   :project nil
   :on-status-changed-fns []})

(defonce db* (atom {:projects {}}))

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
                                (if (clojure.core/get-in projects [(.getBasePath project) :project])
                                  projects
                                  (update projects (.getBasePath project) #(merge (assoc empty-project :project project) %))))))

(defn all-projects []
  (->> @db*
       :projects
       vals
       (mapv :project)
       (remove nil?)
       (remove #(.isDisposed %))))
