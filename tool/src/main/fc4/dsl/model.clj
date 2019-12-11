(ns fc4.dsl.model
  (:require [clj-yaml.core :as yaml]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :refer [starts-with?]]
            [cognitect.anomalies :as anom]
            [expound.alpha :as expound :refer [expound-str]]
            [fc4.spec :as fs]
            [fc4.util :as u :refer [add-ns fault fault? update-all]]
            [fc4.yaml :as fy :refer [split-file]]
            [medley.core :refer [deep-merge map-vals]])
   (:import [org.yaml.snakeyaml.parser ParserException]))

(alias 'fc4.model 'm)

(s/def ::m/description ::fs/non-blank-str) ;; Could reasonably have linebreaks.
(s/def ::comment ::fs/non-blank-str) ;; Could reasonably have linebreaks.

(s/def ::simple-strings
  (s/coll-of ::fs/short-non-blank-simple-str :gen-max 11))

(s/def ::short-simple-keyword
  (s/with-gen
    (s/and keyword?
           (comp (partial s/valid? ::fs/short-non-blank-simple-str) name))
    #(gen/fmap keyword (s/gen ::fs/short-non-blank-simple-str))))

(s/def ::name
  (s/with-gen
    (s/or :string  ::fs/short-non-blank-simple-str
          :keyword ::short-simple-keyword)
    ;; This needs to generate a small and stable set of names so that the
    ;; generated relationships have a chance of being valid — or at least useful.
    #(gen/elements ["A" "B" "C"])))

(s/def ::small-set-of-keywords
  (s/coll-of ::short-simple-keyword
             :distinct true
             :kind set?
             :gen-max 10))

(s/def ::repos (s/coll-of ::fs/short-non-blank-simple-str :gen-max 3))

(s/def ::tag
  (s/with-gen ::fs/short-non-blank-simple-str
    #(gen/one-of [(s/gen ::fs/short-non-blank-simple-str)
                  ; The below tags have special meaning so it’s important that
                  ; they’re sometimes generated.
                  (gen/return "external")
                  (gen/return "internal")])))

(s/def ::tags
  (s/map-of ::tag
            (s/or :string  ::fs/short-non-blank-simple-str
                  :boolean boolean?)
            :distinct true
            :gen-max 5))

(s/def ::system-name ::name)
(s/def ::system-ref ::system-name)
(s/def ::system ::system-ref)

(s/def ::container-ref ::name)
(s/def ::container ::container-ref)

(s/def ::protocol ::fs/non-blank-simple-str)

(s/def ::relationship-purpose ::fs/non-blank-str)
(s/def ::to   ::relationship-purpose)
(s/def ::for  ::relationship-purpose)
(s/def ::what ::relationship-purpose)

(s/def ::uses
  (s/map-of ::name
            (s/keys :req [::to] :opt [::container ::protocol])
            :min-elements 1 :max-gen 2))

(s/def ::depends-on
  (s/map-of ::name
            (s/keys :req [::for] :opt [::container ::protocol])
            :min-elements 1 :max-gen 2))

(s/def ::reads-from
  (s/map-of ::name
            (s/keys :req [::what] :opt [::protocol])
            :min-elements 1 :max-gen 2))

(s/def ::writes-to
  (s/map-of ::name
            (s/keys :req [::what] :opt [::protocol])
            :min-elements 1 :max-gen 2))

(s/def ::all-relationships
  (s/keys :opt [::uses ::depends-on ::reads-from ::writes-to]))

(s/def ::element
  (s/keys :opt [::comment ::description ::tags]))

(s/def ::container-map
  (s/merge ::element
           ::all-relationships
           (s/keys :opt [::repos])))

(s/def ::containers
  (s/map-of ::name ::container-map :min-count 1 :gen-max 2))

(s/def ::system-map
  (s/merge ::element
           ::all-relationships
           (s/keys :opt [::containers ::repos ::datastores ::datatypes ::systems])))

(s/def ::user-map
  (s/merge ::element
           ; I could maybe be convinced that the other kinds of relationships
           ; are valid for users, but we’ll see.
           (s/keys :opt [::uses])))

(s/def ::datastore-map
  ; I guess *maybe* a datastore could have a depends-on relationship? Not sure;
  ; I’d prefer to model datastores as fundamentally passive for now.
  (s/merge ::element
           (s/keys :opt [::repos ::datastores ::datatypes])))

(s/def ::datastore
  (s/or :inline-datastore ::datastore-map
        :datastore-ref    ::fs/short-non-blank-simple-str))

(s/def ::refs
  (s/coll-of ::name :distinct true :kind set? :gen-max 10))

(s/def ::sys-refs ::refs)
(s/def ::container-refs ::refs)

(s/def ::publishers  ::sys-refs)
(s/def ::subscribers ::sys-refs)

(s/def ::datatype-map
  (s/merge ::element
           (s/keys :opt [::repos ::datastore])))

;;;; Keys that may appear at the root of the YAML files:
(s/def ::systems    (s/map-of ::name ::system-map    :gen-max 3))
(s/def ::users      (s/map-of ::name ::user-map      :gen-max 3))
(s/def ::datastores (s/map-of ::name ::datastore-map :gen-max 3))
(s/def ::datatypes  (s/map-of ::name ::datatype-map  :gen-max 3))

;; “Root map” of model YAML files. This is slightly different from a model, in that:
;; * A model file may contain only a single root key, whereas a model must have all the root keys
;; * A model file may contain a root-level tags key, to be applied to every element in the file,
;;   but a model doesn’t support this
(s/def ::file-map
  (s/keys :req-un [(or ::systems ::users ::datastores ::datatypes)]
          :opt-un [::systems ::users ::datastores ::datatypes
                   ;; tags to be applied to every element in the file
                   ::tags]))

(s/def ::file-map-yaml-string
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
    #(gen/fmap yaml/generate-string (s/gen ::file-map))))

(defn- postprocess-keys
  "First qualify each keyword key using the fc4.dsl.model namespace. Then check if a corresponding
  spec exists for the resulting qualified keyword. If it does, then replace the key with the
  qualified key. If it does not, then use the string version of the keyword, because it’s not a
  “keyword” of the DSL, so it’s probably a name or a tag name (key)."
  [m]
  (update-all
   (fn [[k v]]
     (let [qualified (add-ns "fc4.dsl.model" k)]
       (if (s/get-spec qualified)
         [qualified v]
         [(name k) v])))
   m))

(defn parse-file
  "Given a YAML model file as a string, parses it, and qualifies all map keys
  except those at the root so that the result has a chance of being a valid
  ::file-map. If a file contains “top matter” then only the main document is
  parsed. Performs very minimal validation. If the file contains malformed YAML,
  or does not contain a map, an anomaly will be returned."
  [file-contents]
  (try
    (let [parsed (-> (split-file file-contents)
                     (::fy/main)
                     (yaml/parse-string))]
      (if (associative? parsed)
        (map-vals postprocess-keys parsed)
        (fault "Root data structure must be a map (mapping).")))
    (catch ParserException e
      (fault (str "YAML could not be parsed: error " e)))))

(s/fdef parse-file
  :args (s/cat :v (s/alt :valid-and-well-formed ::file-map-yaml-string
                         :invalid-or-malformed  string?))
  :ret  (s/or :valid-and-well-formed ::file-map
              :invalid-or-malformed  ::anom/anomaly)
  :fn   (fn [{{arg :v} :args, ret :ret}]
          (= (first arg) (first ret))))

(defn validate-parsed-file
  "Returns either an error message as a string or nil."
  [parsed]
  (cond
    (s/valid? ::file-map parsed)
    nil

    (fault? parsed)
    (::anom/message parsed)

    :else
    (expound-str ::file-map parsed)))

(s/fdef validate-parsed-file
  :args (s/cat :parsed (s/alt :valid   ::file-map
                              :invalid map?))
  :ret  (s/or                 :valid   nil?
                              :invalid string?)
  :fn   (fn [{{arg :parsed} :args, ret :ret}]
          (= (first arg) (first ret))))
