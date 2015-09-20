(ns happy.interceptors)

(defn default-headers-interceptor
  [[req om :as m]]
  (if-let [hm (get-in m [:default-headers (:method req)])]
    [(update req :headers #(merge hm %)) om]
    m))

(defn now
  []
  #?(:clj (System/currentTimeMillis)
     :cljs (.now js/window.performance)))

(defn timing-interceptor
  [[req om]]
  (let [i (now)]
    [req (update om :response-interceptors #(cons %2 %1) (fn [m _] (assoc m :timing (- (now) i))))]))