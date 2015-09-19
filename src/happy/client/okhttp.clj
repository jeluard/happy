(ns happy.client.okhttp
  (:require [clojure.string :as string]
            [happy.core :as h :refer [Client RequestHandler ResponseHandler]]
            [happy.headers :as hea])
  (:import [com.squareup.okhttp
            Call Callback
            OkHttpClient
            Headers
            MediaType
            Request Request$Builder RequestBody
            Response ResponseBody]
           [java.io File IOException InterruptedIOException]
           [java.util.concurrent TimeUnit]))

; TODO add progress support
; request: https://gist.github.com/lnikkila/d1a4446b93a0185b0969
; response: https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/com/squareup/okhttp/recipes/Progress.java

(defn method->string [k] (string/upper-case (name k)))

(def ^:const ba (type (byte-array [])))
(defn- byte-array? [o] (instance? ba o))

(defn create-body
  [m o]
  (if-let [ct (hea/content-type m)]
    (if-let [^MediaType mt (MediaType/parse (first ct))]
      (cond
        (string? o) (RequestBody/create mt ^String o)
        (byte-array? o) (RequestBody/create mt ^bytes o)
        (instance? File o) (RequestBody/create mt ^File o))
      (throw (ex-info "Unsupported body type" {:body o})))
    (throw (ex-info "Can't have a body without content-type" {:body o}))))

(defn ^Request create-request
  [{:keys [url method body headers]}]
  (let [b (Request$Builder.)]
    (.url b ^String url)
    (doseq [[^String k ^String v] headers]
      (.addHeader b k v))
    (.method b (method->string method) (if body (create-body headers body)))
    (.build b)))

(defn exception->termination
  [^IOException ioe]
  (cond
    (instance? InterruptedIOException ioe) (h/failure :timeout (.toString ioe))
    :else (h/failure :network (.toString ioe))))

(defn headers
  [^Headers o]
  (reduce #(assoc %1 %2 (let [l (.values o %2)] (if (= 1 (count l)) (first l) (vec l)))) {} (.names o)))

(deftype OkHTTPResponse
  [^Response resp body-as]
  ResponseHandler
  (-status [_] (.code resp))
  (-body [_]
    (with-open [^ResponseBody b (.body resp)]
      (cond
        (or (nil? body-as) (= :string body-as)) (.string b)
        (= :byte-array body-as) (.bytes b)
        (= :stream body-as) (.byteStream b))))
  (-header [_ s] (.header resp s))
  (-headers [_] (headers (.headers resp))))

(defn create-callback
  [_ {:keys [handler response-body-as] :as m}]
  (reify Callback
    (onResponse [_ resp]
      (h/finalize handler (h/response (OkHTTPResponse. resp response-body-as)) m))
    (onFailure [_ _ ioe]
      (h/finalize handler (exception->termination ioe) m))))

(deftype OkHTTPRequestHandler
  [^Call ca f m]
  RequestHandler
  (-abort [_] (.cancel ca) (h/finalize f (h/failure :abort) m)))

(defn send!
  [req {:keys [timeout connect-timeout read-timeout write-timeout] :as m}]
  (let [^OkHttpClient  c (OkHttpClient.)
        o (create-request req)
        ^Call ca (.newCall c o)]
    (when (or timeout connect-timeout read-timeout write-timeout)
      (.setConnectTimeout c (or timeout connect-timeout) TimeUnit/MILLISECONDS)
      (.setReadTimeout c read-timeout TimeUnit/MILLISECONDS)
      (.setWriteTimeout c write-timeout TimeUnit/MILLISECONDS))
    (.enqueue ca (create-callback req m))
    (OkHTTPRequestHandler. ca (:handler m) m)))

(defn create
  []
  (reify Client
    (-supports [_]
      {:timeout #{:connect :read :write}
       :request-body-as #{:string :byte-array :file}
       :response-body-as #{:string :byte-array :stream}})
    (-send! [_ req m]
      (send! req m))))