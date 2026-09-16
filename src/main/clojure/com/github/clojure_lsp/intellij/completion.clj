(ns com.github.clojure-lsp.intellij.completion
  "Customizes LSP4IJ completion so `completionItem/resolve` never blocks the EDT.

  LSP4IJ's LSPCompletionProposal awaits the resolve response synchronously inside
  getExpensiveRenderer (called on EDT while the typing write action is held) and
  in computeDocumentation. Both incoming and outgoing LSP messages are processed
  by the same single LSP4IJ thread, so if that thread is waiting for a read action
  the response never arrives and the IDE deadlocks until the freeze watchdog kills it.
  IntelliJ runs the renderer returned by getExpensiveRenderer in a cancellable
  background read action, so we resolve there instead."
  (:require
   [clojure.string :as string]
   [com.github.ericdallo.clj4intellij.logger :as logger]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.intellij.codeInsight.completion LookupElementListPresenter]
   [com.intellij.codeInsight.lookup LookupElement LookupElementPresentation LookupElementRenderer LookupManager]
   [com.intellij.openapi.editor Editor]
   [com.intellij.openapi.progress ProcessCanceledException]
   [com.intellij.platform.backend.documentation DocumentationResult DocumentationResult$Documentation]
   [com.intellij.psi PsiFile]
   [com.redhat.devtools.lsp4ij LanguageServerItem]
   [com.redhat.devtools.lsp4ij.client.features LSPCompletionFeature LSPCompletionFeature$LSPCompletionContext LSPCompletionProposal]
   [com.redhat.devtools.lsp4ij.features.documentation LSPDocumentationHelper]
   [java.lang.reflect InvocationTargetException Method]
   [java.util.function Supplier]
   [org.eclipse.lsp4j CompletionItem]))

(set! *warn-on-reflection* true)

(def ^:private resolve-method
  "LSPCompletionProposal#getResolvedCompletionItem is private. It lazily sends
  completionItem/resolve, caches the future and awaits it checking for cancellation,
  so calling it from a background thread lets LSP4IJ's own handleInsert reuse the
  resolved item afterwards without blocking the EDT."
  (delay
    (try
      (doto (.getDeclaredMethod LSPCompletionProposal "getResolvedCompletionItem" (make-array Class 0))
        (.setAccessible true))
      (catch Exception e
        (logger/warn "Could not access LSPCompletionProposal#getResolvedCompletionItem, completion items won't be resolved asynchronously:" (str e))
        nil))))

(defn ^:private resolve-item
  "Sends completionItem/resolve if needed and blocks the current thread until the
  server replies or the current progress is cancelled. Must not be called from EDT."
  ^CompletionItem [^LSPCompletionProposal proposal]
  (when-let [^Method method @resolve-method]
    (try
      (.invoke method proposal (object-array 0))
      (catch InvocationTargetException e
        (throw (.getCause e))))))

(defn ^:private selected? [^LSPCompletionProposal proposal ^Editor editor]
  (let [lookup (LookupManager/getActiveLookup editor)]
    (and (instance? LookupElementListPresenter lookup)
         (identical? proposal (some-> (.getCurrentItem ^LookupElementListPresenter lookup) .getObject)))))

(defn ^:private expensive-renderer
  "Same behavior as LSP4IJ's getExpensiveRenderer, except that the resolve happens
  inside the returned renderer, which IntelliJ runs in a background non-blocking
  read action, instead of on the EDT."
  [^LSPCompletionProposal proposal ^Editor editor]
  (when (and @resolve-method
             (.needToResolveCompletionDetail proposal)
             (selected? proposal editor))
    (proxy+ [] LookupElementRenderer
      (renderElement [_ ^LookupElement _element ^LookupElementPresentation presentation]
        (let [item (.getItem proposal)]
          (try
            (when-let [detail (some-> (resolve-item proposal) .getDetail)]
              (.setDetail item detail))
            (catch ProcessCanceledException e
              (throw e))
            (catch Exception e
              (logger/warn "Error resolving completion item" (.getLabel item) (str e))))
          (when-not (.getDetail item)
            ;; avoid resolving again the same item
            (.setDetail item ""))
          (.renderElement proposal presentation))))))

(defn ^:private documentation
  ^DocumentationResult$Documentation [^CompletionItem item ^PsiFile file]
  (let [contents (LSPDocumentationHelper/getValidMarkupContents item)
        ^LanguageServerItem language-server nil]
    (when-not (.isEmpty contents)
      (DocumentationResult/documentation (LSPDocumentationHelper/convertToHtml contents language-server file)))))

(defn ^:private compute-documentation
  "LSP4IJ only returns documentation when the item was already resolved, otherwise
  it builds an async documentation but discards it. Resolve asynchronously instead."
  [^LSPCompletionProposal proposal ^LSPCompletionFeature$LSPCompletionContext context ^PsiFile file]
  (let [item (.getItem proposal)]
    (if (or (.getDocumentation item)
            (not (.isResolveCompletionSupported context))
            (not @resolve-method))
      (documentation item file)
      (let [^Supplier supplier (reify Supplier
                                 (get [_]
                                   (some->> (resolve-item proposal) .getDocumentation (.setDocumentation item))
                                   (documentation item file)))]
        (DocumentationResult/asyncDocumentation supplier)))))

(defn ^:private completion-proposal
  ^LookupElement [^CompletionItem item ^LSPCompletionFeature$LSPCompletionContext context ^LSPCompletionFeature feature]
  (let [parameters (.getParameters context)
        editor (.getEditor parameters)
        file (.getOriginalFile parameters)]
    (proxy+ [item context feature] LSPCompletionProposal
      (getExpensiveRenderer [^LSPCompletionProposal this]
        (expensive-renderer this editor))
      (computeDocumentation [^LSPCompletionProposal this]
        (compute-documentation this context file)))))

(defn completion-feature
  "LSPCompletionFeature whose lookup elements resolve completion items off the EDT."
  ^LSPCompletionFeature []
  (proxy+ [] LSPCompletionFeature
    (createLookupElement [^LSPCompletionFeature this ^CompletionItem item ^LSPCompletionFeature$LSPCompletionContext context]
      (when-not (string/blank? (.getLabel item))
        (completion-proposal item context this)))))
