(ns com.github.clojure-lsp.intellij.test-utils 
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [com.github.ericdallo.clj4intellij.test :as clj4intellij.test])
  (:import 
   [com.intellij.openapi.wm WindowManager]
   [com.intellij.ide DataManager]
   [com.intellij.openapi.actionSystem ActionManager]))

(set! *warn-on-reflection* true)

(defn get-status-bar-widget [project widget-id]
  (let [status-bar (.. (WindowManager/getInstance) (getStatusBar project))]
    (.getWidget status-bar widget-id)))

(defn run-editor-action [action-id project]
  (let [action (.getAction (ActionManager/getInstance) action-id)
        context (.getDataContext (DataManager/getInstance))]
    (println "Running action:" action-id)
    (app-manager/write-command-action
     project
     (fn []
       (.actionPerformed
        action
        (com.intellij.openapi.actionSystem.AnActionEvent/createFromDataContext action-id nil context))))))

(defn dispatch-all-until
  [{:keys [project millis timeout]
    :or {millis 1000
         timeout 10000}}]
  (let [start-time (System/currentTimeMillis)]
    (loop []
      (let [current-time (System/currentTimeMillis)
            elapsed-time (- current-time start-time)
            _ (println "Elapsed time >> " elapsed-time)
            status (lsp-client/server-status project)]
        (cond
          (>= elapsed-time timeout)
          (throw (ex-info "LSP server failed to start within timeout"
                          {:elapsed-time elapsed-time
                           :final-status status}))

          (= status :started)
          true

          :else
          (do
            (clj4intellij.test/dispatch-all)
            (Thread/sleep millis)
            (recur)))))))