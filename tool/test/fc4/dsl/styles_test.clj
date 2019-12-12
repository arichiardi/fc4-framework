(ns fc4.dsl.styles-test
  (:require [clojure.test :refer [deftest is]]
            [fc4.styles :as st]
            [fc4.test-utils :refer [check]]))

(deftest styles-from-file (check `st/styles-from-file 1000))
