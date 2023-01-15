(ns com.github.clojure-lsp.intellij.db)

(set! *warn-on-reflection* true)

(def ^:private initial-db
  {:status :disconnected
   :on-status-changed-fns []
   :client nil
   :server nil
   :project nil})

(defonce db* (atom initial-db))
