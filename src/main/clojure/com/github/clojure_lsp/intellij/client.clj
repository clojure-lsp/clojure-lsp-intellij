(ns com.github.clojure-lsp.intellij.client
  (:require
   [clojure.core.async :as async]
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.clojure-lsp.intellij.logger :as logger]
   [lsp4clj.coercer :as coercer]
   [lsp4clj.lsp.requests :as lsp.requests]
   [lsp4clj.lsp.responses :as lsp.responses]
   [lsp4clj.protocols.endpoint :as protocols.endpoint]))

(set! *warn-on-reflection* true)

(defmulti show-message identity)
(defmulti show-message-request identity)
(defmulti progress (fn [_context {:keys [token]}] token))
(defmulti workspace-apply-edit (fn [{:keys [label]}] label))

(defn ^:private publish-diagnostics [{:keys [uri diagnostics]}]
  (swap! db/db* assoc-in [:diagnostics uri] diagnostics))

(defn ^:private receive-message
  [client context message]
  (let [message-type (coercer/input-message-type message)]
    (try
      (let [response
            (case message-type
              (:parse-error :invalid-request)
              (protocols.endpoint/log client :red "Error reading message" message-type)
              :request
              (protocols.endpoint/receive-request client context message)
              (:response.result :response.error)
              (protocols.endpoint/receive-response client message)
              :notification
              (protocols.endpoint/receive-notification client context message))]
        ;; Ensure client only responds to requests
        (when (identical? :request message-type)
          response))
      (catch Throwable e
        (protocols.endpoint/log client :red "Error receiving:" e)
        (throw e)))))

;; TODO move to lsp4clj
(defrecord Client [client-id
                   input output
                   log-ch
                   join
                   request-id
                   sent-requests]
  protocols.endpoint/IEndpoint
  (start [this context]
    (protocols.endpoint/log this :white "lifecycle:" "starting")
    (let [pipeline (async/pipeline-blocking
                    1 ;; no parallelism preserves server message order
                    output
                     ;; TODO: return error until initialize request is received? https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#initialize
                     ;; `keep` means we do not reply to responses and notifications
                    (keep #(receive-message this context %))
                    input)]
      (async/go
        ;; wait for pipeline to close, indicating input closed
        (async/<! pipeline)
        (deliver join :done)))
    ;; invokers can deref the return of `start` to stay alive until server is
    ;; shut down
    join)
  (shutdown [this]
    (protocols.endpoint/log this :white "lifecycle:" "shutting down")
    ;; closing input will drain pipeline, then close output, then close
    ;; pipeline
    (async/close! input)
    (if (= :done (deref join 10e3 :timeout))
      (protocols.endpoint/log this :white "lifecycle:" "shutdown")
      (protocols.endpoint/log this :red "lifecycle:" "shutdown timed out"))
    (async/close! log-ch))
  (log [this msg params]
    (protocols.endpoint/log this :white msg params))
  (log [_this _color msg params]
    ;; TODO apply color
    (async/put! log-ch (string/join " " [msg params])))
  (send-request [this method body]
    (let [req (lsp.requests/request (swap! request-id inc) method body)
          p (promise)
          start-ns (System/nanoTime)]
      (protocols.endpoint/log this :cyan "sending request:" req)
      ;; Important: record request before sending it, so it is sure to be
      ;; available during receive-response.
      (swap! sent-requests assoc (:id req) {:request p
                                            :start-ns start-ns})
      (async/>!! output req)
      p))
  (send-notification [this method body]
    (let [notif (lsp.requests/notification method body)]
      (protocols.endpoint/log this :blue "sending notification:" notif)
      (async/>!! output notif)))
  (receive-response [this {:keys [id] :as resp}]
    (if-let [{:keys [request start-ns]} (get @sent-requests id)]
      (let [ms (float (/ (- (System/nanoTime) start-ns) 1000000))]
        (protocols.endpoint/log this :green (format "received response (%.0fms):" ms) resp)
        (swap! sent-requests dissoc id)
        (deliver request (if (:error resp)
                           resp
                           (:result resp))))
      (protocols.endpoint/log this :red "received response for unmatched request:" resp)))
  (receive-request [this _ {:keys [id method params] :as req}]
    (protocols.endpoint/log this :magenta "received request:" req)
    (when-let [response-body (case method
                               "window/showMessageRequest" (show-message-request params)
                               "workspace/applyEdit" (workspace-apply-edit params)
                               (logger/warn "Unknown LSP request method %s" method))]
      (let [resp (lsp.responses/response id response-body)]
        (protocols.endpoint/log this :magenta "sending response:" resp)
        resp)))
  (receive-notification [this context {:keys [method params] :as notif}]
    (protocols.endpoint/log this :blue "received notification:" notif)
    (case method
      "window/showMessage" (show-message params)
      "$/progress" (progress context params)
      "textDocument/publishDiagnostics" (publish-diagnostics params)

      (logger/warn "Unknown LSP notification method %s" method))))

(defn client [server]
  (map->Client
   {:client-id 1
    :input (:output-ch server)
    :output (:input-ch server)
    :log-ch (async/chan (async/sliding-buffer 20))
    :join (promise)
    :sent-requests (atom {})
    :request-id (atom 0)}))

(defn start-server-and-client! [server client context]
  ((requiring-resolve 'clojure-lsp.server/start-server!) server)
  (protocols.endpoint/start client context)
  (async/go-loop []
    (when-let [log-args (async/<! (:log-ch client))]
      (logger/info log-args)
      (recur))))

(defn request! [client [method body]]
  (protocols.endpoint/send-request client (subs (str method) 1) body))

(defn notify! [client [method body]]
  (protocols.endpoint/send-notification client (subs (str method) 1) body))
