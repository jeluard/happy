(ns happy.representor.json
  (:require [happy.core :as core]
            [happy.representors :refer [Representator]]
            #?(:clj [cheshire.core :as che])))

(defn serialize
  [m]
  #?(:clj (che/generate-string m)
     :cljs (.stringify js/JSON (clj->js m))))

(defn unserialize
  [s keywordize-keys?]
  #?(:clj (che/parse-string s keywordize-keys?)
     :cljs (js->clj (.parse js/JSON s) :keywordize-keys keywordize-keys?)))

(defn create
  ([] (create false))
  ([keywordize-keys?]
   (reify Representator
     (-mime-types [_] #{"application/json"})
     (-serialize [_ o] (serialize o))
     (-unserialize [_ s] (unserialize s keywordize-keys?)))))

(defn merge-representors!
  [keywordize-keys?]
  (core/merge-representors!
    [(create keywordize-keys?)]))
