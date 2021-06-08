(ns deja-fu
  (:require [lambdaisland.deja-fu :as t]))


(t/parse-local-date-time (str (goog.date.DateTime. (js/Date. 2021 0 1 23 59 ))))
(t/parse-local-date-time (str (goog.date.DateTime. (js/Date. 2000 1 1 0 0 0 ))))


