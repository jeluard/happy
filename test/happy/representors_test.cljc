(ns happy.representors-test
  (:require #?(:clj [clojure.test :refer [deftest is]]
               :cljs [cljs.test :as t])
            [happy.representors :as repr])
  #?(:cljs (:require-macros [cljs.test :refer [deftest is]])))

(deftest valid?
  (is (true? (repr/valid? "application/json" "application/json")))
  (is (true? (repr/valid? "application/json" "application/vnd.api+json")))
  (is (false? (repr/valid? "application/json" "application/not-json")))
  (is (true? (repr/valid? "image/png" "image/png")))
  (is (true? (repr/valid? "image/*" "image/png"))))

(deftest matching-representor
  (is (= repr/text-representor (repr/matching-representor {:headers {"content-type" "text/html"}} [repr/text-representor])))
  (is (= repr/text-representor (repr/matching-representor {:headers {"content-type" "text/text"}} [repr/text-representor])))
  (is (= repr/binary-representor (repr/matching-representor {:headers {"content-type" "audio/snd"}} [repr/binary-representor])))
  (is (nil? (repr/matching-representor {:headers {"content-type" "application/unknow"}} [])))
  (is (nil? (repr/matching-representor {} [repr/text-representor]))))