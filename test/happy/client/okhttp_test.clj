(ns happy.client.okhttp-test
  (:require [clojure.test :refer [deftest is]]
            [happy.client.okhttp :as ok]
            [happy.client.specs :as sp]))

(deftest client-specs
  (sp/specs (ok/create)))