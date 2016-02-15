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
  [req v mt]
  (if-let [ct (or mt (first (hea/content-type (:headers req))))]
    (some #(valid-for-mime? ct %) v)))

(defn as-request-interceptor
  [v]
  (fn [[req om :as o]]
    (if (contains? req :body)
      (if-let [r (matching-representor req v (:override-request-mime-type om))]
        (assoc o 0 (update req :body #(-serialize r %)))))))

(defn as-response-interceptor
  [v]
  (fn [resp om]
    (if (contains? resp :body)
      (if-let [r (matching-representor resp v (:override-response-mime-type om))]
        (update resp :body #(-unserialize r %))))))

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
