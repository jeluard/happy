(ns happy.client.specs
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :as t])
            [happy.core :as h])
  #?(:cljs (:require-macros [cljs.test :refer [deftest is testing]])))

(defn specs
  [c]
  (testing "Simple request"
    (is (not (nil? (h/send! {:method "GET" :url "http://google.com"} {:client c}))))
    (is (not (nil? (h/send! {:method "PUT" :url "http://www.mocky.io/v2/5185415ba171ea3a00704eed"
                             :body "payload" :headers {"content-type" "text/text"}} {:client c}))))))