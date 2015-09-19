(ns happy.client.xmlhttprequest
  (:require [clojure.string :as string]
            [happy.core :as h :refer [Client RequestHandler ResponseHandler]]))

; A Client implementation for browsers based on https://xhr.spec.whatwg.org

(defn reduce-headers
  [m line]
  (let [[k v] (string/split line #":" 2)
        n (string/lower-case (string/trim k))
        v (string/trim v)]
    (if-let [ov (get m n)]
      (assoc m n (conj (if (vector? ov) ov (vector ov)) v))
      (assoc m n v))))

(defn parse-headers
  [s]
  (let [headers (string/replace s #"\n$" "")]
    (reduce reduce-headers {} (string/split-lines headers))))

(defn method->string [k] (string/upper-case (name k)))

(defn progress-details
  [evt]
  (if (.-lengthComputable evt)
    {:loaded (.-loaded evt) :total (.-total evt)}))

(deftype XHRRequestHandler
  [xhr]
  RequestHandler
  (-abort [_] (.abort xhr)))

(deftype XHRResponseHandler
  [xhr]
  ResponseHandler
  (-status [_] (.-status xhr))
  (-body [_] (.-response xhr))
  (-header [_ s] (.getResponseHeader xhr s))
  (-headers [_] (parse-headers (.getAllResponseHeaders xhr))))

(defn response-type
  [s]
  (case s
    :array-buffer "arraybuffer"
    :blob "blob"))

(defn send!
  [{:keys [url method headers body]} {:keys [handler with-credentials? timeout report-progress? response-body-as] :as m}]
  (let [xhr (js/XMLHttpRequest.)
        rh (XHRResponseHandler. xhr)
        s (method->string method)]
    (if with-credentials? (set! (.-withCredentials xhr) true))
    (if (and response-body-as (not= response-body-as :string))
      (set! (.-responseType xhr) (response-type response-body-as)))
    (if timeout (set! (.-timeout xhr) timeout))
    (.open xhr s url true)
    (doseq [[k v] headers]
      (.setRequestHeader xhr k v))
    (when handler
      ; load, abort, error and timeout are mutually exclusive
      (set! (.-onload xhr) #(h/finalize handler (h/response rh) m))
      (set! (.-onabort xhr) #(h/finalize handler (h/failure :abort) m))
      (set! (.-onerror xhr) #(h/finalize handler (h/failure :network) m))
      (if timeout
        (set! (.-ontimeout xhr) #(h/finalize handler (h/failure :timeout) m)))
      (when report-progress?
        (set! (.-onprogress xhr) #(handler (h/progress :receiving (merge {:response rh} (progress-details %)))))
        (set! (.-onreadystatechange xhr) #(let [i (.. % -target -readyState)] (if (= 2 i) (handler (h/progress :headers-received)))))
        (if body
          (set! (.. xhr -upload -onprogress) #(handler (h/progress :sending (progress-details %)))))))
    (if body
      (.send xhr body)
      (.send xhr))
    (XHRRequestHandler. xhr)))

(defn create
  []
  (reify Client
    (-supports [_]
      {:progress true
       :timeout true
       :request-body-as #{:string :blob :buffer-source}
       :response-body-as #{:string :blob :array-buffer}
       :extra-options #{:with-credentials?}})
    (-send! [_ req m]
      (send! req m))))