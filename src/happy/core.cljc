(ns happy.core
  (:require [happy.representors :as repr]
            [happy.representor.json :as reprj]))

(defprotocol Client
  (-supports [_])
  (-send! [_ req m] "Returns a RequestHandler"))

(defprotocol RequestHandler
  (-abort [_]))

(defprotocol ResponseHandler
  (-status [_])
  (-body [_])
  (-header [_ s])
  (-headers [_]))

; Options handling

(def default-options (atom nil))

(defn swap-options!
  [f & args]
  (apply swap! default-options f args))

(defn default-option-combiner
  [r l]
  "Combines seq by concatening them.
   For all others types the new value takes precedence."
  (cond
    (sequential? l) (concat l r)
    (map? l) (merge-with default-option-combiner l r)
    :else (or l r)))

(defn merge-options!
  ([m] (merge-options! m default-option-combiner))
  ([m f]
   (swap-options! #(merge-with f %1 %2) m)))

(defn reset-options!
  []
  (reset! default-options nil))

(defn set-default-client!
  [c]
  (swap-options! assoc :client c))

; Utils methods for Client implementations

(defn apply-interceptors
  [o v]
  (if v
    (reduce (fn [o f] (f o)) o v)
    o))

(defn apply-request-interceptors
  [req om]
  (apply-interceptors [req om] (:request-interceptors om)))

(defn apply-response-interceptors
  [resp om]
  (apply-interceptors resp (:response-interceptors om)))

(defn progress
  ([t] (progress t nil))
  ([t m]
   (let [b {:type :progress :change t}]
     (if m
       (merge b m)
       b))))

(defn finalize
  [f resp m]
  (if f
    (f (apply-response-interceptors resp m))))

(defn response
  [r]
  {:type :response
   :status (-status r)
   :body (-body r)
   :headers (-headers r)})

(defn failure
  ([t] (failure t nil))
  ([t s]
   (let [m {:type :failure
            :termination t}]
     (if s
       (assoc m :reason s)
       m))))

(defn validate-request!
  [req]
  (let [met (:method req)]
    (if (and (#{"POST" "PUT" "PATCH"} met) (nil? (:body req)))
      (throw (ex-info (str "Method " met " requires a body" ) req)))
    (if (and (#{"GET" "HEAD" "OPTIONS"} met) (not (nil? (:body req))))
      (throw (ex-info (str "Method " met " requires no body" ) {}))))
  (if-not (every? #(and (string? (key %)) (string? (val %))) (:headers req))
    (throw (ex-info "Headers must be a String / String map" {}))))

(defn validate-options!
  [m]
  (if-let [c (:client m)]
    (let [sm (-supports c)]
      (if-let [as (:request-body-as m)]
        (if-not ((:request-body-as sm) as)
          (throw (ex-info (str "Unsupported :request-body-as : " as) {:m m}))))
      (if-let [as (:response-body-as m)]
        (if-not ((:response-body-as sm) as)
          (throw (ex-info (str "Unsupported :response-body-as : " as) {:m m})))))
    (throw (ex-info "No :client set" {:m m}))))

(defn send!
  [req m]
  (let [f (or (:default-option-combiner m) (:default-option-combiner @default-options) default-option-combiner)
        [req m] (apply-request-interceptors req (merge-with f @default-options m))
        c (:client m)]
    (validate-request! req)
    (validate-options! m)
    (-send! c req m)))

(defn GET
  ([url] (GET url {}))
  ([url hm] (GET url hm nil))
  ([url hm m]
   (send! {:method "GET" :url url :headers hm} m)))

(defn HEAD
  ([url] (HEAD url {}))
  ([url hm] (HEAD url hm nil))
  ([url hm m]
   (send! {:method "HEAD" :url url :headers hm} m)))

(defn POST
  ([url b] (POST url {} b))
  ([url hm b] (POST url hm b nil))
  ([url hm b m]
   (send! {:method "POST" :url url :headers hm :body b} m)))

(defn PUT
  ([url b] (PUT url {} b))
  ([url hm b] (PUT url hm b nil))
  ([url hm b m]
   (send! {:method "PUT" :url url :headers hm :body b} m)))

(defn PATCH
  ([url b] (PATCH url {} b))
  ([url hm b] (PATCH url hm b nil))
  ([url hm b m]
   (send! {:method "PATCH" :url url :headers hm :body b} m)))

(defn DELETE
  ([url] (DELETE url {}))
  ([url hm] (DELETE url hm nil))
  ([url hm m]
   (send! {:method "DELETE" :url url :headers hm} m)))

(defn OPTIONS
  ([url] (OPTIONS url {}))
  ([url hm] (OPTIONS url hm nil))
  ([url hm m]
   (send! {:method "OPTIONS" :url url :headers hm} m)))

; Setup default options
(merge-options!
  {:request-interceptors [(repr/as-request-interceptor [(reprj/create) repr/text-representor repr/binary-representor])]
   :response-interceptors [(repr/as-response-interceptor [(reprj/create) repr/text-representor repr/binary-representor])]})