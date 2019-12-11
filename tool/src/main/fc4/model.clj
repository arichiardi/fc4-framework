(ns fc4.model
  "Functions and specs for working with models in memory.
   The representation of a model in memory, as defined by ::model, is an extension of the
   representation of the model in the DSL. For example, while in the DSL a model may not define any
   datasets and would therefore not contain the key :datasets, the model here _must_ contain that
   key, so as to make it easier to work with the data in code."
  (:require [clojure.spec.alpha :as s]
            [fc4.dsl.model :as dsl]))

;;; TODO: EXPLAIN
(s/def ::proto-model
  (s/keys :req [::dsl/systems ::dsl/users ::dsl/datastores ::dsl/datatypes]))

(s/def ::model
  (s/and ::proto-model
         ; A valid model must have at least one system and at least one user.
         #(seq (::dsl/systems %))
         #(seq (::dsl/users %))))
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
(defn empty-model
  []
  {::dsl/systems {} ::dsl/users {} ::dsl/datastores {} ::dsl/datatypes {}})
