(ns com.github.clojure-lsp.intellij.project-lsp
  (:require
   [clojure.java.io :as io]
   [com.github.clojure-lsp.intellij.client :as lsp-client])
  (:import
   [com.intellij.openapi.project Project]))

(set! *warn-on-reflection* true)

(defn clojure-project?
  "If a project file was not opened before, we didn't start the server yet,
   so we guess if it's Clojure project checking for common deps management files."
  [^Project project]
  (or (not (= :none (lsp-client/server-status project)))
      (let [project-path (.getBasePath project)]
        (or (.exists (io/file project-path "deps.edn"))
            (.exists (io/file project-path "project.clj"))
            (.exists (io/file project-path "shadow-cljs.edn"))
            (.exists (io/file project-path "bb.edn"))
            (.exists (io/file project-path "build.boot"))))))
