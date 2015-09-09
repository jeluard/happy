(ns happy.client.xmlhttprequest-test
  (:require [cljs.test :as t]
            [happy.client.xmlhttprequest :as xhr])
  (:require-macros [cljs.test :refer [deftest is]]))

(deftest zz
  (is (= {"content-type" "application/json" "origin" "localhost"}
         (xhr/parse-headers "content-type: application/json\n origin: localhost")))
  (is (= {"vary" ["content-type" "content-encoding"]}
         (xhr/parse-headers "vary: content-type\n vary: content-encoding"))))