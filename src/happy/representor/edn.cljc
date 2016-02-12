(ns happy.representor.edn
  (:require #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as reader])
            [happy.representors :refer [Representator]]))

(defn create
  ([] (create nil))
  ([m]
   (reify Representator
     (-mime-types [_] #{"application/edn"})
     (-serialize [_ o] (pr-str o))
     (-unserialize [_ o]
       (if (string? o)
         #?(:clj (edn/read-string m o) :cljs (reader/read-string o))
         #?(:clj (edn/read m o) :cljs (reader/read o nil nil nil)))))))
