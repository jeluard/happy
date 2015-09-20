(ns happy.representors
  (:require [happy.headers :as hea]
            [happy.mime :as mim]))

(defprotocol Representator
  (-mime-types [_])
  (-serialize [_ o])
  (-unserialize [_ o]))

(defn valid?
  [s t]
  (if (mim/wildcard? s)
    (= (mim/top-level s) (mim/top-level t))
    (= s (mim/generic t))))

(defn valid-for-mime?
  [s r]
  (some #(if (valid? % s) r) (-mime-types r)))

(defn matching-representor
  [m v]
  (if-let [ct (hea/content-type (:headers m))]
    (some #(valid-for-mime? (first ct) %) v)))

(defn serialize
  [r o]
  (-serialize r o))

(defn as-request-interceptor
  [v]
  (fn [[req _ :as o]]
    (if-let [r (matching-representor req v)]
      (assoc o 0 (update req :body #(serialize r %)))
      o)))

(defn unserialize
  [r o]
  (-unserialize r o))

(defn as-response-interceptor
  [v]
  (fn [resp]
    (if-let [r (matching-representor resp v)]
      (update resp :body #(unserialize r %))
      resp)))

(def binary-representor
  (reify Representator
    (-mime-types [_] #{"image/*" "audio/*" "video/*"})
    (-serialize [_ s] s)
    (-unserialize [_ s] s)))

(def text-representor
  (reify Representator
    (-mime-types [_] #{"text/*"})
    (-serialize [_ s] s)
    (-unserialize [_ s] s)))