(ns happy.representor.transit
  (:require [clojure.string :as string]
            [happy.representors :refer [Representator]]
            [cognitect.transit :as t])
  #?(:clj (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
                   [java.nio.charset Charset])))

#?(:cljs (def w (t/writer :json)))

#?(:cljs (def r (t/reader :json)))

#?(:clj (def ^:private charset (Charset/forName "UTF-8")))

(defn serialize
  [o]
  #?(:clj (let [os (ByteArrayOutputStream. 4096)
                w (t/writer os :json)]
             (t/write w o)
             (.toString os))
     :cljs (t/write w o)))

(defn unserialize
  [s]
  #?(:clj (t/read (t/reader (ByteArrayInputStream. (.getBytes ^String s ^Charset charset)) :json))
     :cljs (when-not (string/blank? s) (t/read r s))))

(defn create
  []
  (reify Representator
    (-mime-types [_] #{"application/transit+json"})
    (-serialize [_ o] (serialize o))
    (-unserialize [_ s] (unserialize s))))