(ns com.github.clojure-lsp.intellij.factory
  (:import
   (portal.extensions.intellij WithLoader))
  (:gen-class
   :main false
   :extends portal.extensions.intellij.WithLoader
   :name com.github.clojure-lsp.intellij.Factory))

(defn start-nrepl []
  (try
    (requiring-resolve 'nrepl.server/start-server)
    (catch Exception _e)))

(defn -init [_this]
  (WithLoader/bind)
  (when-let [start-server (start-nrepl)] (start-server :port 6660)))
