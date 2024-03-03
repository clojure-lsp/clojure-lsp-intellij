(ns com.github.clojure-lsp.intellij.config
  (:require
   [clojure.java.io :as io])
  (:import
   [com.intellij.openapi.project Project]
   [java.io File]))

(set! *warn-on-reflection* true)

(defn ^:private plugin-path* ^File []
  (io/file (com.intellij.openapi.application.PathManager/getPluginsPath) "clojure-lsp"))

(def plugin-path (memoize plugin-path*))

(defn download-server-path ^File []
  (io/file (plugin-path) "clojure-lsp"))

(defn project-cache-path ^File [^Project project]
  (io/file (plugin-path) "cache" (.getName project)))
