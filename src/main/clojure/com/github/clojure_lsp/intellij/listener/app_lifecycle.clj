(ns com.github.clojure-lsp.intellij.listener.app-lifecycle
  (:gen-class
   :name com.github.clojure_lsp.intellij.listener.AppLifecycleListener
   :extends com.github.clojure_lsp.intellij.WithLoader
   :implements [com.intellij.ide.AppLifecycleListener])
  (:import
   [com.github.clojure_lsp.intellij WithLoader]))

(defn -appFrameCreated [_ _] (WithLoader/bind))

(defn -welcomeScreenDisplayed [_])
(defn -appStarted [_])
(defn -projectFrameClosed [_])
(defn -projectOpenFailed [_])
(defn -appClosing [_])
(defn -appWillBeClosed [_ _])
