(ns com.github.clojure-lsp.intellij.clojure-module-builder-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [com.github.clojure-lsp.intellij.extension.clojure-module-builder :as module-builder]
   [com.github.ericdallo.clj4intellij.test :as clj4intellij.test])
  (:import
   [com.github.clojure_lsp.intellij.extension ClojureModuleBuilder]
   [com.intellij.openapi.module Module]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.roots ModuleRootManager]
   [com.intellij.openapi.vfs LocalFileSystem VfsUtilCore VirtualFile]
   [com.intellij.testFramework EdtTestUtil]
   [com.intellij.util ThrowableRunnable]))

(set! *warn-on-reflection* true)

(defn ^:private write-template
  "Writes files the way the real template copy does, with java.io and
   behind the back of the VFS."
  [_template project-path _project-name]
  (doseq [[path content] {"deps.edn" "{:paths [\"src\"]}"
                          "src/new_project/main.clj" "(ns new-project.main)"
                          "test/new_project/main_test.clj" "(ns new-project.main-test)"}]
    (let [file (io/file project-path path)]
      (io/make-parents file)
      (spit file content))))

(defn ^:private relative-paths [^VirtualFile root files]
  (set (map #(VfsUtilCore/getRelativePath ^VirtualFile % root) files)))

(deftest new-project-test
  (let [fixtures (clj4intellij.test/setup-heavy "clojure-new-project")
        ^Project project (.getProject fixtures)
        base-path (.getBasePath project)
        _ (.mkdirs (io/file base-path))
        base-dir (.refreshAndFindFileByPath (LocalFileSystem/getInstance) base-path)
        builder (doto (ClojureModuleBuilder.)
                  (.setName "new-project")
                  (.setModuleFilePath (str base-path "/new-project.iml")))
        module* (atom nil)]
    ;; Load the children of the project dir into the VFS before the template
    ;; is written, as the New Project wizard does when it creates `.idea`.
    (is (empty? (filter #(#{"src" "test" "deps.edn"} (.getName ^VirtualFile %))
                        (.getChildren base-dir))))
    (with-redefs [module-builder/copy-template write-template]
      (EdtTestUtil/runInEdtAndWait
       (reify ThrowableRunnable
         (run [_]
           (reset! module* (.commitModule builder project nil))))))
    (let [^Module module @module*
          root-manager (ModuleRootManager/getInstance module)]
      (testing "the module has the project dir as content root"
        (is (= [base-dir] (vec (.getContentRoots root-manager)))))
      (testing "the VFS sees the template files"
        (is (some? (.findFileByRelativePath base-dir "deps.edn")))
        (is (some? (.findFileByRelativePath base-dir "src/new_project/main.clj")))
        (is (some? (.findFileByRelativePath base-dir "test/new_project/main_test.clj"))))
      (testing "src and test are source roots"
        (is (= #{"src"} (relative-paths base-dir (.getSourceRoots root-manager false))))
        (is (= #{"src" "test"} (relative-paths base-dir (.getSourceRoots root-manager true))))))))
