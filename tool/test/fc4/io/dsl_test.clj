(ns fc4.io.dsl-test
  (:require [clojure.java.io      :as f :refer [file]]
            [clojure.spec.alpha   :as s]
            [clojure.test         :as ct :refer [deftest is testing]]
            [cognitect.anomalies  :as anom]
            [expound.alpha        :as ex :refer [expound-str]]
            [fc4.io.dsl           :as dsl])
  (:import [java.io FileNotFoundException]))

(u/namespaces '[fc4 :as f])

(defn child-dirs
  [dir-path]
  (->> (file dir-path)
       (.listFiles)
       (filter #(.isDirectory %))))

(deftest read-model
  (testing "happy paths:"
    (testing "valid models"
      (let [valid-model-dirs (child-dirs "test/data/models/valid")]
        (is (seq valid-model-dirs))
        (doseq [dir (child-dirs "test/data/models/valid")]
          (let [result (dsl/read-model dir)]
            (is (s/valid? ::f/model result)
                (if-let [msg (::anom/message result)]
                  msg
                  (expound-str ::f/model result)))))))

    (testing "valid and nested models should be equal once read"
      (is (= (dsl/read-model "test/data/models/valid/valid-a-flat")
             (dsl/read-model "test/data/models/valid/valid-a-nested")))))

  (testing "sad path:"
    (testing "files on disk contain invalid data as per the specs"
      (let [result (dsl/read-model "test/data/models/invalid/invalid-a")]
        (is (not (s/valid? ::f/model result)))
        (is (s/valid? ::dsl/error result))))

    (testing "a file is malformed (it is not valid YAML)"
      (let [result (dsl/read-model "test/data/models/invalid/malformed")]
        (is (not (s/valid? ::f/model result)))
        (is (s/valid? ::dsl/error result))))

    (testing "dir does not exist:"
      (is (thrown-with-msg? FileNotFoundException
                            #"root"
                            (dsl/read-model "foo/bar/root"))))

    (testing "supplied root path is to a file"
      (is (thrown-with-msg? RuntimeException #"not a dir"
                            (dsl/read-model "test/data/styles (valid).yaml"))))))

(deftest read-view
  (testing "happy path"
    (is (s/valid? ::f/view
                  (dsl/read-view "test/data/views/middle (valid).yaml"))))

  (testing "sad path:"
    (testing "file on disk contains invalid data as per the specs"
      (let [result (dsl/read-view "test/data/views/middle (invalid).yaml")]
        (is (not (s/valid? ::f/view result)))
        (is (s/valid? ::dsl/error result))))

    (testing "file does not exist"
      (is (thrown-with-msg? FileNotFoundException #"foo"
                            (dsl/read-view "foo"))))))

(deftest read-styles
  (testing "happy path"
    (is (s/valid? ::f/styles
                  (dsl/read-styles "test/data/styles (valid).yaml"))))

  (testing "sad path:"
    (testing "file on disk contains invalid data as per the specs"
      (let [result (dsl/read-styles "test/data/styles (invalid).yaml")]
        (is (not (s/valid? ::f/styles result)))
        (is (s/valid? ::dsl/error result))))

    (testing "file does not exist"
      (is (thrown-with-msg? FileNotFoundException #"foo"
                            (dsl/read-styles "foo"))))))
