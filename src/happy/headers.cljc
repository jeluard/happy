(ns happy.headers
  (:require [clojure.string :as string])
  #?(:clj (:import [java.text SimpleDateFormat]
                   [java.util Locale])))

(defn parse-date
  [s]
  #?(:clj
     (let [format (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss ZZZ" Locale/US)]
       (.parse format s))
     :cljs (js/Date. s)))

(defn content-type
  [hm]
  (if-let [s (get hm "content-type")]
    (string/split (string/trim s) #"[ ]*;[ ]*")))
