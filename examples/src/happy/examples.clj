(ns happy.examples
  (:require [happy.core :as h :refer [GET]]
            [happy.client.okhttp :as ok]))

(h/merge-options! {:report-progress? true
                   :handler #(println %)})

(h/set-default-client! (ok/create))

(defn -main
  []
  (GET "http://www.google.com" {} {:timeout 10}))