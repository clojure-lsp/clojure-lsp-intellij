(ns project-name.main-test
  (:require
   [clojure.test :refer [deftest is]]
   [project-name.main :as main]))

(deftest main-test
  (is (= 0 (main/-main))))
