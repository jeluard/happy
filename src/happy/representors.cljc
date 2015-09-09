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
  (some #(valid-for-mime? (first (hea/content-type (:headers m))) %) v))

(defn as-request-interceptor
  [v]
  (fn [m _]
    (if-let [r (matching-representor m v)]
      (update m :body #(-serialize r %))
      m)))

(defn as-response-interceptor
  [v]
  (fn [m _]
    (if-let [r (matching-representor m v)]
      (update m :body #(-unserialize r %))
      m)))

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