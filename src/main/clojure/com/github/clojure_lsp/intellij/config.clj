(ns com.github.clojure-lsp.intellij.config
  (:require
   [clojure.java.io :as io])
  (:import
   [java.io File]))

(set! *warn-on-reflection* true)

(defn ^:private plugin-path* ^File []
  (io/file (com.intellij.openapi.application.PathManager/getPluginsPath) "clojure-lsp"))

(def plugin-path (memoize plugin-path*))

(defn download-server-path ^File []
  (io/file (plugin-path) "clojure-lsp"))

(defn download-server-version-path ^File []
  (io/file (plugin-path) "clojure-lsp-version"))
