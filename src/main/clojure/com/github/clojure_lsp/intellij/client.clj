(ns com.github.clojure-lsp.intellij.client
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [cheshire.core :as json]
   [clojure.core.async :as async]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [com.github.clojure-lsp.intellij.db :as db]
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [lsp4clj.coercer :as coercer]
   [lsp4clj.lsp.requests :as lsp.requests]
   [lsp4clj.lsp.responses :as lsp.responses]
   [lsp4clj.protocols.endpoint :as protocols.endpoint])
  (:import
   [com.intellij.openapi.project Project]
   [java.io
    EOFException
    IOException
    InputStream
    OutputStream]))

(set! *warn-on-reflection* true)

(defmulti show-message (fn [_context args] args))
(defmulti show-message-request identity)
(defmulti progress (fn [_context {:keys [token]}] token))
(defmulti workspace-apply-edit (fn [_context {:keys [label]}] label))

(defn ^:private publish-diagnostics [{:keys [project]} {:keys [uri diagnostics]}]
  (db/assoc-in project [:diagnostics uri] diagnostics))

(defn ^:private receive-message
  [client context message]
  (logger/info "receiving response ---------->" message)
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
                   log-ch
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
      (async/go
        ;; wait for pipeline to close, indicating input closed
        (async/<! pipeline)
        (logger/info "pipeline closed --------->")
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
      (logger/info "adding to output-ch ---------->" req)
      (async/>!! output-ch req)
      (logger/info "added to output-ch ---------->" req)
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

(defn ^:private kw->camelCaseString
  "Convert keywords to camelCase strings, but preserve capitalization of things
  that are already strings."
  [k]
  (cond-> k (keyword? k) csk/->camelCaseString))

(def ^:private write-lock (Object.))

(defn ^:private write-message [^OutputStream output msg]
  (let [content (json/generate-string (cske/transform-keys kw->camelCaseString msg))
        content-bytes (.getBytes content "utf-8")]
    (locking write-lock
      (doto output
        (logger/info "----------->" (-> (str "Content-Length: " (count content-bytes) "\r\n"
                                             "\r\n")
                                        (.getBytes "US-ASCII")))
        (logger/info "----------->" content-bytes)
        (.write (-> (str "Content-Length: " (count content-bytes) "\r\n"
                         "\r\n")
                    (.getBytes "US-ASCII"))) ;; headers are in ASCII, not UTF-8
        (.write content-bytes)
        (.flush)))))

(defn output-stream->output-chan
  "Returns a channel which expects to have messages put on it. nil values are
  not allowed. Serializes and writes the messages to the output. When the
  channel is closed, closes the output.

  Writes in a thread to avoid blocking a go block thread."
  [output]
  (let [output (io/output-stream output)
        messages (async/chan)]
    (async/thread
      (with-open [writer output] ;; close output when channel closes
        (loop []
          (logger/info "checking msg --------->")
          (if-let [msg (async/<!! messages)]
            (do
              (logger/info "got msg --------->" msg)
              (try
                (write-message writer msg)
                (catch Throwable e
                  (logger/info "error ------------>" e)
                  (async/close! messages)
                  (throw e)))
              (recur))
            (logger/info "nao caiu no let ------------>")))))
    messages))

(defn ^:private read-n-bytes [^InputStream input content-length charset-s]
  (let [buffer (byte-array content-length)]
    (loop [total-read 0]
      (when (< total-read content-length)
        (let [new-read (.read input buffer total-read (- content-length total-read))]
          (when (< new-read 0)
            ;; TODO: return nil instead?
            (throw (EOFException.)))
          (recur (+ total-read new-read)))))
    (String. ^bytes buffer ^String charset-s)))

(defn ^:private parse-header [line headers]
  (let [[h v] (string/split line #":\s*" 2)]
    (assoc headers h v)))

(defn ^:private parse-charset [content-type]
  (or (when content-type
        (when-let [[_ charset] (re-find #"(?i)charset=(.*)$" content-type)]
          (when (not= "utf8" charset)
            charset)))
      "utf-8"))

(defn ^:private read-message [input headers keyword-function]
  (try
    (let [content-length (Long/valueOf ^String (get headers "Content-Length"))
          charset-s (parse-charset (get headers "Content-Type"))
          content (read-n-bytes input content-length charset-s)]
      (json/parse-string content keyword-function))
    (catch Exception _
      :parse-error)))

(defn ^:private read-header-line
  "Reads a line of input. Blocks if there are no messages on the input."
  [^InputStream input]
  (try
    (let [s (java.lang.StringBuilder.)]
      (loop []
        (let [b (.read input)] ;; blocks, presumably waiting for next message
          (case b
            -1 ::eof ;; end of stream
            #_lf 10 (str s) ;; finished reading line
            #_cr 13 (recur) ;; ignore carriage returns
            (do (.append s (char b)) ;; byte == char because header is in US-ASCII
                (recur))))))
    (catch IOException _e
      ::eof)))

(defn input-stream->input-chan
  "Returns a channel which will yield parsed messages that have been read off
  the `input`. When the input is closed, closes the channel. By default when the
  channel closes, will close the input, but can be determined by `close?`.

  Reads in a thread to avoid blocking a go block thread."
  ([input] (input-stream->input-chan input {}))
  ([input {:keys [close? keyword-function]
           :or {close? true, keyword-function csk/->kebab-case-keyword}}]
   (let [input (io/input-stream input)
         messages (async/chan)]
     (async/thread
       (loop [headers {}]
         (let [line (read-header-line input)]
           (cond
             ;; input closed; also close channel
             (= line ::eof) (async/close! messages)
             ;; a blank line after the headers indicates start of message
             (string/blank? line) (if (async/>!! messages (read-message input headers keyword-function))
                                    ;; wait for next message
                                    (recur {})
                                    ;; messages closed
                                    (when close? (.close input)))
             :else (recur (parse-header line headers))))))
     messages)))

(defn client [in out]
  (map->Client
   {:client-id 1
    :input-ch (input-stream->input-chan out)
    :output-ch (output-stream->output-chan in)
    :log-ch (async/chan (async/sliding-buffer 20))
    :join (promise)
    :sent-requests (atom {})
    :request-id (atom 0)}))

(defn start-client! [client context]
  (protocols.endpoint/start client context)
  (async/go-loop []
    (when-let [log-args (async/<! (:log-ch client))]
      (logger/info log-args)
      (recur))))

(defn request! [client [method body]]
  (protocols.endpoint/send-request client (subs (str method) 1) body))

(defn notify! [client [method body]]
  (protocols.endpoint/send-notification client (subs (str method) 1) body))

(defn connected-client [^Project project]
  (when (identical? :connected (db/get-in project [:status]))
    (db/get-in project [:client])))
