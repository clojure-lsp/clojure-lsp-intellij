(ns com.github.clojure-lsp.intellij.project
  (:require
   [clojure.java.io :as io])
  (:import
   [com.intellij.openapi.project Project]))

(set! *warn-on-reflection* true)

(defn project->root-uri ^String [^Project project]
  (-> (.getBasePath project) io/file .toPath .toUri str))

(defn clojure-project?
  "If a project file was not opened before, we didn't start the server yet,
   so we guess if it's Clojure project checking for common deps management files."
  [^Project project db]
  (or (not (identical? :disconnected (:status db)))
      (let [project-path (.getBasePath project)]
        (or (.exists (io/file project-path "deps.edn"))
            (.exists (io/file project-path "project.clj"))
            (.exists (io/file project-path "shadow-cljs.edn"))
            (.exists (io/file project-path "bb.edn"))
            (.exists (io/file project-path "build.boot"))))))
