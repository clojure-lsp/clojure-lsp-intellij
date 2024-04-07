(ns com.github.clojure-lsp.intellij.client
  (:require
   [clojure.core.async :as async]
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [lsp4clj.coercer :as coercer]
   [lsp4clj.io-chan :as io-chan]
   [lsp4clj.lsp.requests :as lsp.requests]
   [lsp4clj.lsp.responses :as lsp.responses]
   [lsp4clj.protocols.endpoint :as protocols.endpoint])
  (:import
   [com.intellij.openapi.project Project]))

(set! *warn-on-reflection* true)

(defmulti show-message (fn [_context args] args))
(defmulti show-message-request identity)
(defmulti progress (fn [_context {:keys [token]}] token))
(defmulti workspace-apply-edit (fn [_context {:keys [label]}] label))

(defn ^:private publish-diagnostics [{:keys [project]} {:keys [uri diagnostics]}]
  (db/assoc-in project [:diagnostics uri] diagnostics))

(defn ^:private receive-message
  [client context message]
  (let [message-type (coercer/input-message-type message)]
    (try
      (let [response
            (case message-type
              (:parse-error :invalid-request)
              (protocols.endpoint/log client :red "Error reading message" message)
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

(defrecord Client [client-id
                   input-ch
                   output-ch
                   join
                   request-id
                   sent-requests]
  protocols.endpoint/IEndpoint
  (start [this context]
    (protocols.endpoint/log this :white "lifecycle:" "starting")
    (let [pipeline (async/pipeline-blocking
                    1 ;; no parallelism preserves server message order
                    output-ch
                     ;; TODO: return error until initialize request is received? https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#initialize
                     ;; `keep` means we do not reply to responses and notifications
                    (keep #(receive-message this context %))
                    input-ch)]
      (async/thread
        ;; wait for pipeline to close, indicating input closed
        (async/<!! pipeline)
        (deliver join :done)))
    ;; invokers can deref the return of `start` to stay alive until server is
    ;; shut down
    join)
  (shutdown [this]
    (protocols.endpoint/log this :white "lifecycle:" "shutting down")
    ;; closing input will drain pipeline, then close output, then close
    ;; pipeline
    (async/close! input-ch)
    (if (= :done (deref join 10e3 :timeout))
      (protocols.endpoint/log this :white "lifecycle:" "shutdown")
      (protocols.endpoint/log this :red "lifecycle:" "shutdown timed out")))
  (log [this msg params]
    (protocols.endpoint/log this :white msg params))
  (log [_this _color msg params]
    ;; TODO apply color
    (logger/info (string/join " " [msg params])))
  (send-request [this method body]
    (let [req (lsp.requests/request (swap! request-id inc) method body)
          p (promise)
          start-ns (System/nanoTime)]
      (protocols.endpoint/log this :cyan "sending request:" req)
      ;; Important: record request before sending it, so it is sure to be
      ;; available during receive-response.
      (swap! sent-requests assoc (:id req) {:request p
                                            :start-ns start-ns})
      (async/>!! output-ch req)
      p))
  (send-notification [this method body]
    (let [notif (lsp.requests/notification method body)]
      (protocols.endpoint/log this :blue "sending notification:" notif)
      (async/>!! output-ch notif)))
  (receive-response [this {:keys [id] :as resp}]
    (if-let [{:keys [request start-ns]} (get @sent-requests id)]
      (let [ms (float (/ (- (System/nanoTime) start-ns) 1000000))]
        (protocols.endpoint/log this :green (format "received response (%.0fms):" ms) resp)
        (swap! sent-requests dissoc id)
        (deliver request (if (:error resp)
                           resp
                           (:result resp))))
      (protocols.endpoint/log this :red "received response for unmatched request:" resp)))
  (receive-request [this context {:keys [id method params] :as req}]
    (protocols.endpoint/log this :magenta "received request:" req)
    (when-let [response-body (case method
                               "window/showMessageRequest" (show-message-request params)
                               "workspace/applyEdit" (workspace-apply-edit context params)
                               (logger/warn "Unknown LSP request method" method))]
      (let [resp (lsp.responses/response id response-body)]
        (protocols.endpoint/log this :magenta "sending response:" resp)
        resp)))
  (receive-notification [this context {:keys [method params] :as notif}]
    (protocols.endpoint/log this :blue "received notification:" notif)
    (case method
      "window/showMessage" (show-message context params)
      "$/progress" (progress context params)
      "textDocument/publishDiagnostics" (publish-diagnostics context params)

      (logger/warn "Unknown LSP notification method" method))))

(defn client [in out]
  (map->Client
   {:client-id 1
    :input-ch (io-chan/input-stream->input-chan out)
    :output-ch (io-chan/output-stream->output-chan in)
    :join (promise)
    :sent-requests (atom {})
    :request-id (atom 0)}))

(defn start-client! [client context]
  (protocols.endpoint/start client context))

(defn request! [client [method body]]
  (protocols.endpoint/send-request client (subs (str method) 1) body))

(defn notify! [client [method body]]
  (protocols.endpoint/send-notification client (subs (str method) 1) body))

(defn connected-client [^Project project]
  (when (identical? :connected (db/get-in project [:status]))
    (db/get-in project [:client])))
