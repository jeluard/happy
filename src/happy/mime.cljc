(ns happy.mime
  (:require [clojure.string :as string]))

(defn top-level
  [s]
  (first (string/split s #"/")))

(defn subtype
  [s]
  (second (string/split s #"/")))

(defn suffix
  [s]
  (second (string/split s #"\+")))

(defn wildcard?
  [s]
  (= "*" (subtype s)))

(defn generic
  [s]
  (if-let [su (suffix s)]
    (str (top-level s) "/" su)
    s))

(defn binary?
  [s]
  (boolean (#{"video" "audio" "image"} (top-level s))))