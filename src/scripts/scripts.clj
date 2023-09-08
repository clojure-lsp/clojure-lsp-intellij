(ns scripts
  (:require
   [babashka.tasks :refer [shell]]
   [clojure.string :as string]))

(defn ^:private replace-in-file [file regex content]
  (as-> (slurp file) $
    (string/replace $ regex content)
    (spit file $)))

(defn ^:private add-changelog-entry [tag comment]
  (replace-in-file "CHANGELOG.md"
                   #"## \[Unreleased\]"
                   (if comment
                     (format "## [Unreleased]\n\n## %s\n\n- %s" tag comment)
                     (format "## [Unreleased]\n\n## %s" tag))))

(defn ^:private replace-tag [tag]
  (replace-in-file "gradle.properties"
                   #"pluginVersion = [0-9]+.[0-9]+.[0-9]+.*"
                   (format "pluginVersion = %s" tag)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn tag [& [tag]]
  (shell "git fetch origin")
  (shell "git pull origin HEAD")
  (replace-tag tag)
  (add-changelog-entry tag nil)
  (shell "git add gradle.properties CHANGELOG.md")
  (shell (format "git commit -m \"Release: %s\"" tag))
  (shell (str "git tag " tag))
  (shell "git push origin HEAD")
  (shell "git push origin --tags"))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn build-plugin []
  (shell "./gradlew buildPlugin"))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn publish-plugin []
  (shell "./gradlew publishPlugin"))
