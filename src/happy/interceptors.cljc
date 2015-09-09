(ns happy.interceptors)

(defn prepend-interceptor
  [m t f]
  (update-in m [:options t] #(cons %2 %1) f))

(defn default-headers
  [req om]
  (if-let [hm (get-in om [:default-headers (:method req)])]
    (update req :headers #(merge hm %))
    req))

(defn now
  []
  #?(:clj (System/currentTimeMillis)
     :cljs (.now js/window.performance)))

(defn timing-interceptor
  [req _]
  (let [i (now)]
    (prepend-interceptor req :response-interceptors (fn [m _] (assoc m :timing (- (now) i))))))