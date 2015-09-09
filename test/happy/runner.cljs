(ns happy.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [happy.client.xmlhttprequest-test]
            [happy.representors-test]))

(doo-tests 'happy.client.xmlhttprequest-test
           'happy.representors-test)