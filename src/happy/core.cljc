(ns happy.core
  (:require [happy.representors :as repr]
            [happy.representor.json :as reprj]))

(defprotocol Client
  (-supports [_])
  (-send! [_ req m] "Returns a RequestHandler"))

(defprotocol RequestHandler
  (-abort [_])
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

(defn get-option
  ([m t]
   (or (t m) (t @default-options)))
  ([m t f]
   (f (t m) (t @default-options))))

(defn apply-interceptors
  [m om v]
  (reduce (fn [m f] (f m om)) m v))

(defn progress
  ([t] (progress t nil))
  ([t m]
   (let [b {:type :progress :change t}]
     (if m
       (merge b m)
       b))))

(defn finalize
  [f resp m]
  (f (apply-interceptors resp m (:response-interceptors m))))

(defn response
  [rh]
  ; TODO validate response
  {:type :response
   :status (-status rh)
   :body (-body rh)
   :headers (-headers rh)})

(defn failure
  [t]
  {:type :failure
   :termination t})

(defn send!
  [req m]
  ; TODO validate key
  (let [f (or (:default-option-combiner m) (:default-option-combiner @default-options) default-option-combiner)
        m (merge-with f @default-options m)
        req (apply-interceptors req m (:request-interceptors m))]
    (if-let [c (:client m)]
      (-send! c (dissoc req :options) (merge-with f m (:options req)))
      (throw (ex-info "No :client set" {:m m})))))

(defn GET
  ([url] (GET url {}))
  ([url hm] (GET url hm nil))
  ([url hm m]
   (send! {:method :get :url url :headers hm} m)))

(defn HEAD
  ([url] (HEAD url {}))
  ([url hm] (HEAD url hm nil))
  ([url hm m]
   (send! {:method :head :url url :headers hm} m)))

(defn POST
  ([url] (POST url nil))
  ([url b] (POST url {} b))
  ([url hm b] (POST url hm b nil))
  ([url hm b m]
   (send! {:method :post :url url :headers hm :body b} m)))

(defn PUT
  ([url] (PUT url nil))
  ([url b] (PUT url {} b))
  ([url hm b] (PUT url hm b nil))
  ([url hm b m]
   (send! {:method :put :url url :headers hm :body b} m)))

(defn DELETE
  ([url] (DELETE url {}))
  ([url hm] (DELETE url hm nil))
  ([url hm m]
   (send! {:method :delete :url url :headers hm} m)))

(defn OPTIONS
  ([url] (OPTIONS url {}))
  ([url hm] (OPTIONS url hm nil))
  ([url hm m]
   (send! {:method :options :url url :headers hm} m)))

; Setup default options
(merge-options!
  {:request-interceptors [(repr/as-request-interceptor [(reprj/create) repr/text-representor repr/binary-representor])]
   :response-interceptors [(repr/as-response-interceptor [(reprj/create) repr/text-representor repr/binary-representor])]})