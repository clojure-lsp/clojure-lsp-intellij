(ns com.github.clojure-lsp.intellij.listener.project-manager
  (:gen-class
   :name com.github.clojure_lsp.intellij.listener.ProjectManagerListener
   :implements [com.intellij.openapi.project.ProjectManagerListener])
  (:require
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.project :as project]
   [com.github.clojure-lsp.intellij.server :as server])
  (:import
   [com.intellij.openapi.project Project]))

(defn -projectOpened [_ _])
(defn -canCloseProject [_ _])
(defn -projectClosingBeforeSave [_ _])
(defn -projectClosed [_ _])

(defn -projectClosing [_ ^Project project]
  (when (project/clojure-project? project @db/db*)
    (server/shutdown!)))
