(ns com.github.clojure-lsp.intellij.db
  (:require
   [clojure.core.async :as async]))

(set! *warn-on-reflection* true)

(def ^:private initial-db
  {:status :disconnected
   :on-status-changed-fns []
   :on-diagnostics-updated-fns []
   :client nil
   :server nil
   :project nil})

(defonce db* (atom initial-db))

(defmacro await-init [field & body]
  `(async/go-loop [~'value (get @db* ~field)]
     (if ~'value
       ~@body
       (recur (get @db* ~field)))))
