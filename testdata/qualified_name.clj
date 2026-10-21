(ns bar.foo
  (:require
   [clojure.string :as str]))

(def aa 1)

(def b aa)

(str/join "," [])

(let [x 1] (inc x))
