(ns fc4.dsl.view-test
  (:require [clojure.test :refer [deftest is]]
            [fc4.dsl.view :as v]
            [fc4.test-utils :refer [check]]))

(deftest view-from-file (check `v/view-from-file 1000))
(deftest fixup-keys (check `v/fixup-keys 1000))
