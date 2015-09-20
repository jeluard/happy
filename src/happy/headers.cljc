(ns happy.headers
  (:require [clojure.string :as string]))

(defn content-type
  [hm]
  (if-let [s (get hm "content-type")]
    (string/split (string/trim s) #"[ ]*;[ ]*")))