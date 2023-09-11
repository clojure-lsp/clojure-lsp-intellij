(ns com.github.clojure-lsp.intellij.notification
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [com.github.clojure-lsp.intellij.tasks :as tasks]
   [com.github.ericdallo.clj4intellij.app-manager :as app-manager]
   [seesaw.core :as see])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.notification NotificationGroupManager NotificationType]
   [com.intellij.openapi.project Project]))

(set! *warn-on-reflection* true)

(def ^:private message-type->notification-type
  {1 NotificationType/ERROR
   2 NotificationType/WARNING
   3 NotificationType/INFORMATION
   4 NotificationType/INFORMATION})

(defmethod lsp-client/show-message :default [{:keys [type message]}]
  (-> (NotificationGroupManager/getInstance)
      (.getNotificationGroup "Clojure LSP notifications")
      (.createNotification "Clojure LSP" ^String message ^NotificationType (message-type->notification-type type))
      (.notify ^Project (:project @db/db*))))

(defmethod lsp-client/show-message-request :default [{:keys [_type message actions]}]
  @(app-manager/invoke-later!
    {:invoke-fn
     (fn []
       (see/input message
                  :title "Clojure LSP"
                  :type :question
                  :icon Icons/CLOJURE
                  :choices actions
                  :to-string :title))}))

(defmethod lsp-client/progress :default [_ progress]
  (logger/warn "Unknown progress token" progress))

(defmethod lsp-client/progress "lsp-startup" [{:keys [progress-indicator]} {{:keys [title message percentage]} :value}]
  (let [msg (str "LSP: " (or title message))]
    (if percentage
      (tasks/set-progress progress-indicator msg percentage)
      (tasks/set-progress progress-indicator msg))))
