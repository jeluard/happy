(ns happy.interceptors)

(defn default-headers-interceptor
  [[req om :as v]]
  (if-let [hm (get-in om [:default-headers (:method req)])]
    (assoc v 0 (update req :headers #(merge hm %)))
    v))

(defn now
  []
  #?(:clj (System/currentTimeMillis)
     :cljs (.now js/window.performance)))

(defn timing-interceptor
  [[_ om :as v]]
  (let [i (now)]
    (assoc v 1 (update om :response-interceptors #(cons %2 %1) (fn [m _] (assoc m :timing (- (now) i)))))))