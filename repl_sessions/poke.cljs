(ns deja-fu.poke
  (:require [lambdaisland.deja-fu :as fu]))

(t/parse-local-date-time (str (goog.date.DateTime. (js/Date. 2021 0 1 23 59 ))))
(t/parse-local-date-time (str (goog.date.DateTime. (js/Date. 2000 1 1 0 0 0 ))))

(hash (fu/parse-local-date "2021-10-11"))
(fu/parse-local-date "2021-10-11")
(fu/parse-local-date "2021-W39")
(hash (fu/parse-local-date-time "2021-10-5T10:11:12"))
(hash (fu/parse-local-date-time "2021-10-5T10:11:12"))
