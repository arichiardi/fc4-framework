(ns fc4.dsl.styles
  (:require [clj-yaml.core           :as yaml]
            [clojure.spec.alpha      :as s]
            [clojure.spec.gen.alpha  :as gen]
            [fc4.integrations.structurizr.express.spec] ; for side effect: register specs
            [fc4.spec                :as fs]
            [fc4.util                :as u]))

(u/namespaces '[fc4.styles :as ss]
              '[structurizr.style :as sy])

(s/def ::ss/background ::sy/background)
(s/def ::ss/border ::sy/border)
(s/def ::ss/color ::sy/color)
(s/def ::ss/dashed ::sy/dashed)
(s/def ::ss/height ::sy/height)
(s/def ::ss/shape ::sy/shape)
(s/def ::ss/tag ::sy/tag)
(s/def ::ss/type ::sy/type)
(s/def ::ss/width ::sy/width)

(s/def :fc4/style
  (s/keys
   :req [::ss/type ::ss/tag]
   :opt [::ss/background ::ss/border ::ss/color ::ss/dashed ::ss/height ::ss/shape ::ss/width]))

(s/def :fc4/styles (s/coll-of :fc4/style :min-count 1 :gen-max 30))

(defn parse-file
  "Parses the contents of a YAML file, then processes those contents such that
  they conform to :fc4/style."
  [file-contents]
  ;; We could really just pass the entire file contents into qualify-keys,
  ;; as it operates recursively on any and all nested Clojure data
  ;; structures. However, I’ve defined a simplistic function spec for it
  ;; that says that the arg must be a map with unqualified keyword keys.
  ;; Because that function spec is defined, if I turn on spec’s
  ;; instrumentation feature and then this function is called, then if it’s
  ;; passed the expected YAML string — which should contain a seq of maps
  ;; — then it’ll throw. So that’s why map is used below to pass each map
  ;; in the file to qualify-keys individually.
  (map #(u/qualify-keys % 'fc4.styles)
       (yaml/parse-string file-contents)))

(s/def ::yaml-file-str
  (s/with-gen
    ::fs/non-blank-str
    #(gen/fmap yaml/generate-string (s/gen :fc4/styles))))

(s/fdef parse-file
  :args (s/cat :file-contents ::yaml-file-str)
  :ret  :fc4/styles
  :fn   (fn [{{:keys [file-contents]} :args, ret :ret}]
                ;; Unlike the similar function fc4.view/parse-file, this
                ;; needs to parse the strings back from the YAML back into (in
                ;; this case) seqs of ordered maps because otherwise the YAML
                ;; string serializations of the data structures were using
                ;; different orders for their keys, and thus as strings they
                ;; were not equal. When comparing maps, entries are not ordered
                ;; and thus order is irrelevant. It is actually surprising that
                ;; two ordered maps with different orders would be considered
                ;; equivalent but I’ll give the developers (of flatland/ordered)
                ;; the benefit of the doubt that they’ve good reasons for this.
          (= (yaml/parse-string file-contents)
             (yaml/parse-string (yaml/generate-string ret)))))
