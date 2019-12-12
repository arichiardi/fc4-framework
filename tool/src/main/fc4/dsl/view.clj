(ns fc4.dsl.view
  (:require [clj-yaml.core           :as yaml]
            [clojure.spec.alpha      :as s]
            [clojure.spec.gen.alpha  :as gen]
            [fc4.spec                :as fs]
            [fc4.util                :as util :refer [namespaces]]))

(namespaces ['fc4.view :as 'v])

;; You might ask: why copy specs over from a different namespace? It’s because
;; when the views are parsed from YAML files and we end up with non-namespaced
;; keyword keys, and we then post-process them to qualify them with namespaces,
;; it’s impractical to qualify them with _different_ namespaces, so we’re going
;; to qualify them all with _the same_ namespace. Thus, that namespace needs to
;; include definitions for -all- the keys that appear in the YAML files.
(s/def ::v/name ::fs/short-non-blank-simple-str)
(s/def ::v/subject ::v/name)
(s/def ::v/description ::fs/description)
(s/def ::v/coord-string ::fs/coord-string)

(s/def ::v/subject ::v/coord-string)
(s/def ::v/position-map (s/map-of ::v/name ::v/coord-string :gen-max 2))
(s/def ::v/users ::v/position-map)
(s/def ::v/containers ::v/position-map)
(s/def ::v/other-systems ::v/position-map)

(s/def ::v/positions
  (s/keys
  ; You might look at this and think that the keys in the `or` are mutually
  ; exclusive — that a valid value may contain only *one* of those keys. I tested
  ; this though, and that’s not the case. This merely states that in order to
  ; be considered valid, a value must contain at least one of the keys specified
  ; in the `or` — containing more than one, or all of them, is also valid. (I
  ; suppose it might be handy if s/keys supported `not` but in this case that’s
  ; not needed.) (Another possible useful feature for s/keys could be something
  ; like `one-of` as in “only one of”.)
   :req [(and ::subject (or ::users ::containers ::other-systems))]
   :opt [::users ::containers ::other-systems]))

(s/def ::v/control-point-seqs
  (s/coll-of (s/coll-of ::v/coord-string :min-count 1 :gen-max 3)
             :min-count 1
             :gen-max 3))

(s/def ::v/control-point-group
  (s/map-of ::v/name ::v/control-point-seqs
            :min-count 1
            :gen-max 3))

(s/def ::v/system-context ::v/control-point-group)
(s/def ::v/container (s/map-of ::v/name ::v/control-point-group))

(s/def ::v/control-points
  (s/keys
   :req [::v/system-context]
   :opt [::v/container]))

(s/def ::size
  ;; These options come from Structurizr Express, because that’s our current renderer. And yeah,
  ;; that’s leaking an implementation detail through, but I can’t think of a way to avoid this. The
  ;; same applies to styles. I suppose that means we might need to support these values forever, but
  ;; I think maybe we can live with that.
  ;; If you’re wondering why this doesn’t just reference the same value over in
  ;; src/main/fc4/integrations/structurizr/express/spec.clj, it’s because I don’t want this lib to
  ;; depend on that lib, because this lib is part of the “core” fc4 code while that lib is part of
  ;; an “integration” that may have a shorter shelf-life. I also don’t want to load all that code
  ;; at runtime when it’s not really needed.
  #{"A2_Landscape"
    "A2_Portrait"
    "A3_Landscape"
    "A3_Portrait"
    "A4_Landscape"
    "A4_Portrait"
    "A5_Landscape"
    "A5_Portrait"
    "A6_Landscape"
    "A6_Portrait"
    "Legal_Landscape"
    "Legal_Portrait"
    "Letter_Landscape"
    "Letter_Portrait"
    "Slide_16_9"
    "Slide_4_3"})

(s/def :fc4/view
  (s/keys
   :req [::v/subject ::v/positions ::v/control-points ::v/size]
   :opt [::v/description]))

(defn- fixup-keys
  "Finds any keyword keys that contain spaces and/or capital letters and
  replaces them with their string versions, because any such value is likely to
  be an element name, and we need those to be strings."
  [view]
  (util/update-all
   (fn [[k v]]
     (if (and (keyword? k)
              (re-seq #"[A-Z ]" (name k)))
       [(name k) v]
       [k v]))
   view))

(s/fdef fixup-keys
  :args (s/cat :m (s/map-of ::fs/unqualified-keyword any?))
  :ret  (s/map-of (s/or :keyword keyword? :string string?) any?)
  :fn   (fn [{{m :m} :args, ret :ret}]
          (and (= (count m) (count ret))
               (empty? (->> (keys ret)
                            (filter keyword?)
                            (map name)
                            (filter #(re-seq #"[A-Z ]" %)))))))

; We have to capture this at compile time in order for it to have the value we
; want it to; if we referred to *ns* in the body of a function then, because it
; is dynamically bound, it would return the namespace at the top of the stack,
; the “currently active namespace” rather than what we want, which is the
; namespace of this file, because that’s the namespace all our keywords are
; qualified with.
(def ^:private this-ns-name (str *ns*))

(defn view-from-file
  "Parses the contents of a YAML file, then processes those contents such that
  the result conforms to ::view."
  [file-contents]
  (-> (yaml/parse-string file-contents)
      ;; Both the below functions do a walk through the view; this is
      ;; redundant, duplicative, inefficient, and possibly slow. So this right
      ;; here is a potential spot for optimization.
      (fixup-keys)
      (util/qualify-keys this-ns-name)))

(s/def ::v/yaml-file-contents
  (s/with-gen
    ::fs/non-blank-str
    #(gen/fmap yaml/generate-string (s/gen :fc4/view))))

(s/fdef view-from-file
  :args (s/cat :file-contents ::v/yaml-file-contents)
  :ret  ::view)
