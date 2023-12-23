(ns com.github.clojure-lsp.intellij.extension.clojure-module-builder
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.ClojureModuleBuilder
   :extends com.intellij.ide.util.projectWizard.ModuleBuilder
   :exposes-methods {commitModule superCommitModule})
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [com.rpl.proxy-plus :refer [proxy+]]
   [seesaw.core :as s]
   [seesaw.mig :as s.mig])
  (:import
   [com.github.clojure_lsp.intellij Icons]
   [com.intellij.ide.util.projectWizard ModuleBuilder ModuleWizardStep]
   com.intellij.openapi.Disposable
   [com.intellij.openapi.module ModifiableModuleModel ModuleType]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.roots ModifiableRootModel]
   [java.io File]))

(def clojure-module
  (proxy+ ClojureModuleType ["CLOJURE_MODULE"] ModuleType
          (getName [_] "Clojure")
          (getDescription [_] "Create programs using the Clojure language.")
          (getNodeIcon [_ _] Icons/CLOJURE)))

(defn -getName [_] "Clojure")
(defn -getPresentableName [_] "Clojure")
(defn -getBuilderId [_] "CLOJURE_BUILDER")
(defn -getDescription [_] "Create programs using the Clojure language.")
(defn -getNodeIcon [_] Icons/CLOJURE)

(defn -getModuleType [_] clojure-module)

(defn ^:private project-type-option [id label group]
  (s.mig/mig-panel :items [[(s/label :icon (s/icon Icons/CLOJURE)) ""]
                           [(s/radio :text label
                                     :selected? true
                                     :id id
                                     :group group) ""]]))

(defonce ^:private wizzard* (atom {:component nil
                                   :project-type nil
                                   :button-group nil}))

(defn -getCustomOptionsStep [_ _context ^Disposable _parent-disposable]
  (proxy+ ClojureModuleWizard [] ModuleWizardStep
          (getComponent [_]
            (let [project-group (s/button-group)
                  component (s.mig/mig-panel :items [[(s/label "Choose the Clojure project type") "wrap"]
                                                     [(project-type-option :clojure "Clojure (deps.edn)" project-group) "wrap"]
                                                     [(project-type-option :leiningen "Leiningen (project.clj)" project-group) "wrap"]
                                                     [(project-type-option :shadow-cljs "ClojureScript (shadow.cljs)" project-group) "wrap"]
                                                     [(project-type-option :babashka "Babashka (bb.edn)" project-group) "wrap"]
                                                     [(project-type-option :clojure-dart "ClojureDart (deps.edn)" project-group) "wrap"]])]
              (swap! wizzard* #(-> %
                                   (assoc :component component)
                                   (assoc :button-group project-group)))
              component))
          (updateDataModel [_]
            (swap! wizzard* assoc :project-type (s/id-of (s/selection (:button-group @wizzard*)))))))

(defn -setupRootModel [^ModuleBuilder this ^ModifiableRootModel model]
  (.doAddContentEntry this model))

(defn ^:private project-template [project-type]
  (get
   {:clojure "project-templates/clojure"
    :leiningen "project-templates/lein"
    :shadow-cljs "project-templates/shadow-cljs"
    :babashka "project-templates/bb"
    :clojure-dart "project-templates/cljd"}
   project-type))

(defn ^:private normalize-entry-name [entry-name project-template project-name]
  (-> entry-name
      (string/replace (str project-template File/separatorChar) "")
      (string/replace "project_name" (csk/->snake_case project-name))
      (string/replace "project-name" (csk/->kebab-case project-name))
      (string/replace "projectName" (csk/->camelCase project-name))))

(defn ^:private normalize-content [content project-name]
  (-> content
      (string/replace "project_name" (csk/->snake_case project-name))
      (string/replace "project-name" (csk/->kebab-case project-name))
      (string/replace "projectName" (csk/->camelCase project-name))))

(defn ^:private copy-template [project-template project-path project-name]
  ;; TODO find a better way to access jar's resource without doing this.
  ;; We need to update current thread class loader to be able to
  ;; load resource-paths from our plugin.
  (.setContextClassLoader (Thread/currentThread) (.getClassLoader clojure.lang.Symbol))
  (let [connection (.openConnection (io/resource project-template))
        project-root-entry (.getJarEntry connection)]
    (with-open [jar (.getJarFile connection)]
      (doseq [entry (enumeration-seq (.entries jar))]
        (when (and (not (.isDirectory entry))
                   (string/starts-with? (str entry) (str project-root-entry)))
          (with-open [stream (.getInputStream jar entry)]
            (let [content (normalize-content (slurp stream) project-name)
                  new-file (io/file project-path (normalize-entry-name (.getName entry) project-template project-name))]
              (io/make-parents new-file)
              (spit new-file content))))))))

(defn -commitModule [this ^Project project ^ModifiableModuleModel model]
  (let [project-path (.getBasePath project)
        project-name (.getName project)
        template (project-template (:project-type @wizzard*))]
    (copy-template template project-path project-name)
    (.superCommitModule this project model)))
