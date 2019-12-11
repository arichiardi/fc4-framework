(ns fc4.model
  "Functions and specs for working with models in memory.
   The representation of a model in memory, as defined by ::model, is an extension of the
   representation of the model in the DSL. For example, while in the DSL a model may not define any
   datasets and would therefore not contain the key :datasets, the model here _must_ contain that
   key, so as to make it easier to work with the data in code."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :refer [includes?]]
            [cognitect.anomalies :as anom]
            [fc4.dsl.model :as dm]
            [medley.core :refer [deep-merge map-vals]]))

(do (create-ns 'fc4.model)
    (alias 'm 'fc4.model))

;; TODO: do we actually need this?
(def empty-model
  {::m/systems {} ::m/users {} ::m/datastores {} ::m/datatypes {}})

(defn build-model
  "Accepts a sequence of maps read from model YAML files and combines them into
  a single model map. If any name collisions are detected then an anomaly is
  returned. Does not validate the result."
  [model-file-maps]
  (reduce deep-merge empty-model model-file-maps))

(defn ^:private contains-contents?
  "Given a model and the contents of a parsed model DSL yaml file, a ::file-map, returns true if the
  model contains all the contents of the file-map."
  [model file-map]
  ;; Ideally the below would validate *fully* that each element in the file is fully contained in
  ;; the model. However, because an element can be defined in multiple files and therefore the
  ;; resulting element in the model is a composite (a result of deeply merging the various
  ;; definitions) I don’t know how to validate this. I guess I’m just not smart enough. I mean, I
  ;; suspect I could figure it out eventually given enough time — it’d probably have to do with
  ;; depth-first walking the file element and then confirming that the model contains the same value
  ;; at the same path. But I don’t have the time or energy to figure that out right now.
  ;; TODO: figure this out.
  (->> (for [[tk tm] file-map]
         (for [[k _v] tm]
           (contains? (get model tk {}) k)))
       (flatten)
       (every? true?)))

(s/fdef build-model
  :args (s/cat :in (s/coll-of ::dm/file-map))
  :ret  (s/or :success :fc4/model
              :failure ::anom/anomaly)
  :fn   (fn [{{:keys [in]}      :args
              [ret-tag ret-val] :ret}]
          (and
           ; I saw, at least once, a case wherein the return value was both a valid model *and* a
           ; valid anomaly. We don’t want this.
           (not (and (s/valid? :fc4/model ret-val)
                     (s/valid? ::anom/anomaly  ret-val)))
           (case ret-tag
             :success
             (every? #(contains-contents? ret-val %) in)

             :failure
             (includes? (or (::anom/message ret-val) "") "duplicate names")))))
