(ns fc4.dsl.model
  (:require [clj-yaml.core :as yaml]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :refer [includes? starts-with?]]
            [cognitect.anomalies :as anom]
            [expound.alpha :as expound :refer [expound-str]]
            [fc4.spec :as fs]
            [fc4.util :as u :refer [add-ns fault fault? namespaces update-all]]
            [fc4.yaml :as fy :refer [split-file]]
            [medley.core :refer [deep-merge]])
   (:import [org.yaml.snakeyaml.parser ParserException]))

(namespaces ['fc4.model :as 'm])

(s/def ::m/description ::fs/non-blank-str) ;; Could reasonably have linebreaks.
(s/def ::m/comment ::fs/non-blank-str) ;; Could reasonably have linebreaks.

(s/def ::m/simple-strings
  (s/coll-of ::fs/short-non-blank-simple-str :gen-max 11))

(s/def ::m/short-simple-keyword
  (s/with-gen
    (s/and keyword?
           (comp (partial s/valid? ::fs/short-non-blank-simple-str) name))
    #(gen/fmap keyword (s/gen ::fs/short-non-blank-simple-str))))

(s/def ::m/name
  (s/with-gen
    (s/or :string  ::fs/short-non-blank-simple-str
          :keyword ::m/short-simple-keyword)
    ;; This needs to generate a small and stable set of names so that the
    ;; generated relationships have a chance of being valid — or at least useful.
    #(gen/elements ["A" "B" "C"])))

(s/def ::m/small-set-of-keywords
  (s/coll-of ::m/short-simple-keyword
             :distinct true
             :kind set?
             :gen-max 10))

(s/def ::m/repos (s/coll-of ::fs/short-non-blank-simple-str :gen-max 3))

(s/def ::m/tag
  (s/with-gen ::fs/short-non-blank-simple-str
    #(gen/one-of [(s/gen ::fs/short-non-blank-simple-str)
                  ; The below tags have special meaning so it’s important that
                  ; they’re sometimes generated.
                  (gen/return "external")
                  (gen/return "internal")])))

(s/def ::m/tags
  (s/map-of ::m/tag
            (s/or :string  ::fs/short-non-blank-simple-str
                  :boolean boolean?)
            :distinct true
            :gen-max 5))

(s/def ::m/system-name ::m/name)
(s/def ::m/system-ref ::m/system-name)
(s/def ::m/system ::m/system-ref)

(s/def ::m/container-ref ::m/name)
(s/def ::m/container ::m/container-ref)

(s/def ::m/protocol ::fs/non-blank-simple-str)

(s/def ::m/relationship-purpose ::fs/non-blank-str)
(s/def ::m/to   ::m/relationship-purpose)
(s/def ::m/for  ::m/relationship-purpose)
(s/def ::m/what ::m/relationship-purpose)

(s/def ::m/uses
  (s/map-of ::m/name
            (s/keys :req [::m/to] :opt [::m/container ::m/protocol])
            :min-elements 1 :max-gen 2))

(s/def ::m/depends-on
  (s/map-of ::m/name
            (s/keys :req [::m/for] :opt [::m/container ::m/protocol])
            :min-elements 1 :max-gen 2))

(s/def ::m/reads-from
  (s/map-of ::m/name
            (s/keys :req [::m/what] :opt [::m/protocol])
            :min-elements 1 :max-gen 2))

(s/def ::m/writes-to
  (s/map-of ::m/name
            (s/keys :req [::m/what] :opt [::m/protocol])
            :min-elements 1 :max-gen 2))

(s/def ::m/all-relationships
  (s/keys :opt [::m/uses ::m/depends-on ::m/reads-from ::m/writes-to]))

(s/def ::m/element
  (s/keys :opt [::m/comment ::m/description ::m/tags]))

(s/def ::m/container-map
  (s/merge ::m/element
           ::m/all-relationships
           (s/keys :opt [::m/repos])))

(s/def ::m/containers
  (s/map-of ::m/name ::m/container-map :min-count 1 :gen-max 2))

(s/def ::m/system-map
  (s/merge ::m/element
           ::m/all-relationships
           (s/keys :opt [::m/containers ::m/repos ::m/datastores ::m/datatypes ::m/systems])))

(s/def ::m/user-map
  (s/merge ::m/element
           ; I could maybe be convinced that the other kinds of relationships
           ; are valid for users, but we’ll see.
           (s/keys :opt [::m/uses])))

(s/def ::m/datastore-map
  ; I guess *maybe* a datastore could have a depends-on relationship? Not sure;
  ; I’d prefer to model datastores as fundamentally passive for now.
  (s/merge ::m/element
           (s/keys :opt [::m/repos ::m/datastores ::m/datatypes])))

(s/def ::m/datastore
  (s/or :inline-datastore ::m/datastore-map
        :datastore-ref    ::fs/short-non-blank-simple-str))

(s/def ::m/refs
  (s/coll-of ::m/name :distinct true :kind set? :gen-max 10))

(s/def ::m/sys-refs ::m/refs)
(s/def ::m/container-refs ::m/refs)

(s/def ::m/publishers  ::m/sys-refs)
(s/def ::m/subscribers ::m/sys-refs)

(s/def ::m/datatype-map
  (s/merge ::m/element
           (s/keys :opt [::m/repos ::m/datastore])))

;; Root-level keys — for both an :fc4/model and a ::file.
(s/def ::m/systems    (s/map-of ::m/name ::m/system-map    :gen-max 3))
(s/def ::m/users      (s/map-of ::m/name ::m/user-map      :gen-max 3))
(s/def ::m/datastores (s/map-of ::m/name ::m/datastore-map :gen-max 3))
(s/def ::m/datatypes  (s/map-of ::m/name ::m/datatype-map  :gen-max 3))

(s/def :fc4/model
  (s/keys :req [::m/systems ::m/users ::m/datastores ::m/datatypes]))

;; “Root map” of model DSL YAML files. This is similar to a model, with these differences:
;; * A model file may contain only a single root key, whereas a model must have all the root keys
;; * A model file may contain a root-level tags key, to be applied to every element in the file,
;;   but a model doesn’t allow this.
;; This spec is useful because it allows us to validate an individual model file.
(s/def ::file
  (s/keys :req [(or ::m/systems ::m/users ::m/datastores ::m/datatypes)]
          :opt [::m/systems ::m/users ::m/datastores ::m/datatypes
                ;; tags to be applied to every element in the file
                ::m/tags]))

(s/def ::file-yaml-string
  (s/with-gen
    ;; This spec exists mainly for the use of its generator, so valid inputs can
    ;; be generated and passed to parse-model-file during generative testing.
    ;; That said, the predicate also needs to be able to reject obviously
    ;; invalid values, so as to make the conformance of the generated args
    ;; accurate during generative testing. In other words, the specs in the
    ;; fdef for parse-model-file include an s/or, which will sort of “classify”
    ;; an argument according to the first spec that it validates against, so we
    ;; need values like "" and "foo" to be invalid as per this spec.
    (s/and string?
           (fn [v] (some #(starts-with? v %) ["systems" "users" "datastores"])))
    #(gen/fmap yaml/generate-string (s/gen ::file))))

(defn- qualify-known-keys
  "First qualify each keyword key using the fc4.dsl.model namespace. Then check if a corresponding
  spec exists for the resulting qualified keyword. If it does, then replace the key with the
  qualified key. If it does not, then use the string version of the keyword, because it’s not a
  “keyword” of the DSL, so it’s probably a name or a tag name (key)."
  [m]
  (update-all
   (fn [[k v]]
     (let [qualified (add-ns "fc4.model" k)]
       (if (s/get-spec qualified)
         [qualified v]
         [(name k) v])))
   m))

(defn parse-file
  ;; TODO: apply the contents of the root-level :tags key to every element in the file, then remove
  ;; that root-level :tags key.
  "Given a YAML model file as a string, parses it, and qualifies all map keys so that the result has
  a chance of being a valid ::file. If a file contains “top matter” then only the main document is
  parsed. Performs very minimal validation. If the file contains malformed YAML, or does not contain
  a map, an anomaly will be returned."
  [file-contents]
  (try
    (let [parsed (-> (split-file file-contents)
                     (::fy/main)
                     (yaml/parse-string))]
      (if (associative? parsed)
        (qualify-known-keys parsed)
        (fault "Root data structure must be a map (mapping).")))
    (catch ParserException e
      (fault (str "YAML could not be parsed: error " e)))))

(s/fdef parse-file
  :args (s/cat :v (s/alt :valid-and-well-formed ::file-yaml-string
                         :invalid-or-malformed  string?))
  :ret  (s/or :valid-and-well-formed ::file
              :invalid-or-malformed  ::anom/anomaly)
  :fn   (fn [{{arg :v} :args, ret :ret}]
          (= (first arg) (first ret))))

(defn validate-parsed-file
  "Returns either an error message as a string or nil."
  [parsed]
  (cond
    (s/valid? ::file parsed)
    nil

    (fault? parsed)
    (::anom/message parsed)

    :else
    (expound-str ::file parsed)))

(s/fdef validate-parsed-file
  :args (s/cat :parsed (s/alt :valid   ::file
                              :invalid map?))
  :ret  (s/or                 :valid   nil?
                              :invalid string?)
  :fn   (fn [{{arg :parsed} :args, ret :ret}]
          (= (first arg) (first ret))))

(def empty-model
  #::m{:systems {} :users {} :datastores {} :datatypes {}})

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
  :args (s/cat :in (s/coll-of ::file :gen-max 10))
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
