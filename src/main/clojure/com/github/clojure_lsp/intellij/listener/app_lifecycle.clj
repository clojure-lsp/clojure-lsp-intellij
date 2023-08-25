(ns com.github.clojure-lsp.intellij.listener.app-lifecycle
  (:gen-class
   :name com.github.clojure_lsp.intellij.listener.AppLifecycleListener
   :extends com.github.clojure_lsp.intellij.ClojureClassLoader
   :implements [com.intellij.ide.AppLifecycleListener])
  (:import
   [com.github.clojure_lsp.intellij ClojureClassLoader]))

(defn -appFrameCreated [_ _] (ClojureClassLoader/bind))

(defn -welcomeScreenDisplayed [_])
(defn -appStarted [_])
(defn -appStarting [_ _])
(defn -projectFrameClosed [_])
(defn -projectOpenFailed [_])
(defn -appClosing [_])
(defn -appWillBeClosed [_ _])
