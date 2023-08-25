(ns com.github.clojure-lsp.intellij.notification
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.logger :as logger]
   [seesaw.core :as see])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.notification NotificationGroupManager NotificationType]
   [com.intellij.openapi.application ApplicationManager ModalityState]
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
  (let [p (promise)]
    (.invokeLater
     (ApplicationManager/getApplication)
     (reify Runnable
       (run [_]
         (deliver p (see/input message
                               :title "Clojure LSP"
                               :type :question
                               :icon Icons/CLOJURE
                               :choices actions
                               :to-string :title))))
     (ModalityState/any))
    @p))

(comment
  (lsp-client/show-message-request {:type 1 :message "some really long message here to expand the screen and break\nasd\nas"
                                    :actions [{:title "some action"}
                                              {:title "another cool action"}]}))

(defmethod lsp-client/progress :default [_ progress]
  (logger/warn "Unknown progress token %s" progress))
