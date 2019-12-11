(ns fc4.model
  "Functions and specs for working with models in memory.
   The representation of a model in memory, as defined by ::model, is an extension of the
   representation of the model in the DSL. For example, while in the DSL a model may not define any
   datasets and would therefore not contain the key :datasets, the model here _must_ contain that
   key, so as to make it easier to work with the data in code."
  (:require [clojure.spec.alpha :as s]
            [fc4.dsl.model :as md]
            [medley.core :refer [deep-merge map-vals]]))

(s/def :fc4/model
  (s/keys :req [::md/systems ::md/users ::md/datastores ::md/datatypes]))

; (s/def ::model
;   (s/and ::proto-model
         ; A valid model must have at least one system and at least one user.
         ; #(seq (::md/systems %))
         ; #(seq (::md/users %))))
  ;; THIS WAS USED to make the relationships in the model valid. We might need this.
    ; ]
    ; (s/with-gen
    ;   spec
    ;   (fn []
    ;     (gen/fmap
    ;      (fn [m]
    ;          ; let’s make the relationships in the model valid
    ;        (let [sys-names (take 2 (keys (::systems m)))]
    ;          (-> (update m ::systems #(select-keys % sys-names))
    ;              (update-in [::systems (first sys-names) ::uses] empty)
    ;              (update-in [::systems (second sys-names) ::uses]
    ;                         (fn [_] #{{::system (first sys-names)}})))))
    ;      (s/gen spec))))))

;; TODO: do we actually need this?
(def empty-model
  {::md/systems {} ::md/users {} ::md/datastores {} ::md/datatypes {}})

(def ^:private dsl-to-model-maps
  {:systems    ::m/systems
   :users      ::m/users
   :datastores ::m/datastores})

(defn add-file-map
  "Adds the elements from a parsed DSL file to a model. If any of the elements in the file-map are
  already in the model, they’re merged (using medley/deep-merge) because the model DSL supports
  breaking the definition of a (presumably large) system across multiple files."
  [model file-map]
  (reduce
   (fn [model [src dest]]
     (if-let [src-map (get file-map src)]
       (update model dest deep-merge src-map)
       model))
   model
   dsl-to-model-maps))

(defn ^:private contains-contents?
  "Given a model (or proto-model) and the contents of a parsed model DSL yaml
  file, a ::file-map, returns true if the model contains all the contents of
  the file-map."
  [model file-map]
  ;; Ideally the below would validate *fully* that each element in the file is fully contained in
  ;; the file. However, because an element can be defined in multiple files and therefore the
  ;; resulting element in the model is a composite (a result of deeply merging the various
  ;; definitions) I don’t know how to validate this. I guess I’m just not smart enough. I mean, I
  ;; suspect I could figure it out eventually given enough time — it’d probably have to do with
  ;; depth-first walking the file element and then confirming that the model contains the same value
  ;; at the same path. But I don’t have the time or energy to figure that out right now.
  ;; TODO: figure this out.
  (->> (for [[src dest] dsl-to-model-maps]
         (for [[file-elem-name _file-elem-val] (get file-map src)]
           (contains? (get model dest) file-elem-name)))
       (flatten)
       (every? true?)))

(s/fdef add-file-map
  :args (s/cat :pmodel   ::m/proto-model
               :file-map ::file-map)
  :ret  (s/or :success ::m/proto-model
              :failure ::anom/anomaly)
  :fn   (fn [{{:keys [_pmodel file-map]} :args
              [ret-tag ret-val]          :ret}]
          (and
           ; The :ret spec allows the return value to be either a proto-model
           ; or a valid anomaly. If a value is passed to it that is actually
           ; both a valid proto-model *and* a valid anomaly, it will be
           ; considered valid by the spec, and will be tagged as a :success,
           ; but only because :success is the first spec in the or. So let’s
           ; just ensure this doesn’t happen.
           (not (and (s/valid? ::m/proto-model ret-val)
                     (s/valid? ::anom/anomaly  ret-val)))
           (case ret-tag
             ;; TODO: also validate that ret-val contains the contents of pmodel.
             :success
             (contains-contents? ret-val file-map)

             ;; TODO: also validate that the inputs do indeed have duplicate keys
             :failure
             (includes? (or (::anom/message ret-val) "") "duplicate names")))))

(defn build-model
  "Accepts a sequence of maps read from model YAML files and combines them into
  a single model map. If any name collisions are detected then an anomaly is
  returned. Does not validate the result."
  [file-maps]
  (reduce
   (fn [model file-map]
     (let [result (add-file-map model file-map)]
       (if (fault? result)
         (reduced result)
         result)))
   (empty-model)
   file-maps))

(s/fdef build-model
  :args (s/cat :in (s/coll-of ::file-map))
  :ret  (s/or :success ::m/proto-model
              :failure ::anom/anomaly)
  :fn   (fn [{{:keys [in]}      :args
              [ret-tag ret-val] :ret}]
          (and
           ; I saw, at least once, a case wherein the return value was  both a
           ; valid proto-model *and* a valid anomaly. We don’t want this.
           (not (and (s/valid? ::m/proto-model ret-val)
                     (s/valid? ::anom/anomaly  ret-val)))
           (case ret-tag
             :success
             (every? #(contains-contents? ret-val %) in)

             :failure
             (includes? (or (::anom/message ret-val) "") "duplicate names")))))
