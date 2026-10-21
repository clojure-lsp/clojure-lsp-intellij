(ns com.github.clojure-lsp.intellij.extension.paredit-status-bar
  (:require
   [com.github.clojure-lsp.intellij.extension.paredit :as extension.paredit]
   [com.github.clojure-lsp.intellij.project-lsp :as project]
   [com.github.clojure-lsp.intellij.settings :as settings]
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.wm StatusBarWidget StatusBarWidget$TextPresentation StatusBarWidgetFactory]
   [com.intellij.util Consumer]
   [kotlinx.coroutines CoroutineScope]))

(set! *warn-on-reflection* true)

(def-extension PareditStatusBarFactory []
  StatusBarWidgetFactory
  (getId [_] extension.paredit/widget-id)

  (getDisplayName [_] "Clojure Paredit")

  (isAvailable [_ project]
    (project/clojure-project? project))

  (canBeEnabledOn [_ _] true)

  (isConfigurable [_] true)

  (isEnabledByDefault [_] true)

  (disposeWidget [_ _])

  (createWidget ^StatusBarWidget
    ([^StatusBarWidgetFactory this ^Project project ^CoroutineScope _]
     (.createWidget this project))
    ([_ ^Project project]
     (proxy+
      []
      StatusBarWidget
       (ID [_] extension.paredit/widget-id)
       (dispose [_])
       (install [_ _])
       (getPresentation [this] this)
       StatusBarWidget$TextPresentation
       (getText [_]
         (if (settings/paredit-enabled?) "Paredit: on" "Paredit: off"))
       (getAlignment [_] (float 0.5))
       (getTooltipText [_]
         "Clojure structural editing mode, click to toggle")
       (getClickConsumer [_]
         (reify Consumer
           (consume [_ _]
             (extension.paredit/toggle-strict-mode! project))))))))
