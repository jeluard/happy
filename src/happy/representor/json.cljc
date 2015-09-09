(ns happy.representor.json
  (:require [happy.representors :refer [Representator]]))

(defn create
  ([] (create false))
  ([keywordize-keys?]
   (reify Representator
     (-mime-types [_] #{"application/json"})
     (-serialize [_ o] (.stringify js/JSON (clj->js o)))
     (-unserialize [_ s] (js->clj (.parse js/JSON s) {:keywordize-keys keywordize-keys?})))))