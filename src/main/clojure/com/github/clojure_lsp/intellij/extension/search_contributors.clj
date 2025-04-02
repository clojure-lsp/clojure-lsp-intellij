(ns com.github.clojure-lsp.intellij.extension.search-contributors
  (:require
   [com.github.clojure-lsp.intellij.client :as lsp-client]
   [com.github.clojure-lsp.intellij.project-lsp :as project]
   [com.github.ericdallo.clj4intellij.extension :refer [def-extension]]
   [com.rpl.proxy-plus :refer [proxy+]])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.ide.actions.searcheverywhere SearchEverywhereContributor SearchEverywhereContributorFactory]
   [com.intellij.ide.util NavigationItemListCellRenderer]
   [com.intellij.navigation ItemPresentation NavigationItem]
   [com.intellij.openapi.actionSystem AnActionEvent]
   [com.intellij.util Processor]
   [com.redhat.devtools.lsp4ij.features.workspaceSymbol WorkspaceSymbolData]
   [org.eclipse.lsp4j SymbolKind WorkspaceSymbol]))

(set! *warn-on-reflection* true)

(defn ^:private symbol->navigation-item ^NavigationItem
  [^WorkspaceSymbol sym project]
  (proxy+ [(.getName sym)
           (.getKind sym)
           (.getLeft (.getLocation sym))
           (.getClientFeatures (lsp-client/project->ls-server-item project))
           project]
          WorkspaceSymbolData
    (getPresentation [_]
      (proxy+ [] ItemPresentation
        (getPresentableText [_] (.getName sym))
        (getIcon [_ _] Icons/CLOJURE)))))

(def-extension NamespaceSearchContributorFactory []
  SearchEverywhereContributorFactory
  (isAvailable [_ project] (project/clojure-project? project))
  (createContributor [_ ^AnActionEvent event]
    (let [project (.getProject event)]
      (proxy+ NamespaceSearchContributor []
        SearchEverywhereContributor
        (getSearchProviderId [_] "clojure-lsp-namespace-search")
        (getGroupName [_] "Namespaces")
        (getSortWeight [_] 301)
        (showInFindResults [_] true)
        (isShownInSeparateTab [_] true)
        (isEmptyPatternSupported [_] true)
        (fetchElements [_ pattern _indicator ^Processor consumer]
          (let [syms (->> (lsp-client/symbols pattern project)
                          (filter #(identical? SymbolKind/Namespace (.getKind ^WorkspaceSymbol %))))]
            (doseq [sym syms]
              (.process consumer (symbol->navigation-item sym project)))))
        (processSelectedItem [_ ^NavigationItem item _modifiers _search-text]
          (.navigate item true)
          true)
        (getElementsRenderer [_]
          (NavigationItemListCellRenderer.))))))
