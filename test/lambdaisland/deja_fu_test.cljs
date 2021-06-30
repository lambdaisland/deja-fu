(ns lambdaisland.deja-fu-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [clojure.test.check.clojure-test :refer [defspec]]
            [lambdaisland.deja-fu  :as t]
            [goog.date.DateTime]))

(deftest test-days-between
  (testing "Vaidate 'For example, 11:59 p.m. 1/1/21 and 12:01 a.m. 1/2/21 are 0 days apart.'"
    (is (= 0.0
           (t/days-between
            (goog.date.DateTime. (js/Date. 2021 0 1 23 59 ))
            (goog.date.DateTime. (js/Date. 2021 0 2 0 1))))))
  (testing "Two consecutive days return 1 if those days are"
    (testing "ordinary"
      (is (= 1 (t/days-between (t/local-date 2021 1 1) (t/local-date 2021 1 2)) )))
    (testing "around daylight savings"
      (is (= 1 (t/days-between (t/local-date 2021 11 6) (t/local-date 2021 11 7)) )))
    (testing "constructed as goog.date.DateTime objects"
      (is (= 1 (t/days-between (goog.date.DateTime. 2021 0 1) (goog.date.DateTime. 2021 0 2)) ))
      )
    (testing "constructed as js/Date. and then converted to goog.date.DateTime objects"
      (is (= 1 (t/days-between
                (goog.date.DateTime. (js/Date.  2021 0 1))
                (goog.date.DateTime. (js/Date. 2021 0 2))))))
    (testing "one is a goog.date.DateTime and the other is a local-time."
      (is (= 1 (t/days-between
                (goog.date.DateTime. 2021 0 1)
                (t/local-date 2021 1 2)))))))

(deftest test-parse-local-time
  (testing "Verify that times are parsed correctly"
    (testing "when the format is HH:MM:SS"
      (is  (= (t/parse-local-time "23:01:01") (t/->LocalTime 23 1 1 nil )))
      (is  (=  (t/parse-local-time "11:01:01") (t/->LocalTime 11 1 1 nil ))))
    (testing  "when the format is HH:MM:SS.mmm"
      (is  (=  (t/parse-local-time "23:01:01.001") (t/->LocalTime 23 1 1 1e6)))
      (is  (=  (t/parse-local-time "11:01:01.001") (t/->LocalTime 11 1 1 1e6)) ))
    (testing  "when the format is HH:MM:SS.nnnnnn"
      (is  (=  (t/parse-local-time "23:01:01.001000") (t/->LocalTime 23 1 1 1e6) ) )
      (is  (=  (t/parse-local-time "11:01:01.001000") (t/->LocalTime 11 1 1 1e6)) ))) )

(t/parse-local-time (str (t/->LocalTime 11 1 1 1e6)))

(deftest roundtrip
  (let [time-example (t/->LocalTime 11 1 1 0 )]
    (is (= time-example (t/parse-local-time (str time-example))))))

(deftest roundtrip-nanos
  (let [time-example (t/->LocalTime 11 1 1 1e6 )]
    (is (= time-example (t/parse-local-time (str time-example))))))

(defspec test-time->string->time
  100
  (prop/for-all [h (gen/choose 0 24)
                 m (gen/choose 0 60)
                 s (gen/choose 0 60)]
                (= (t/->LocalTime h m s nil)
                  (t/parse-local-time (str (t/->LocalTime h m s nil))) )))

(defspec test-datetime->string->datetime
  100
  (prop/for-all [year  (gen/choose 2000 2040)
                 month (gen/choose 1 12)
                 day (gen/choose 1 28)
                 hour (gen/choose 0 24)
                 minute (gen/choose 0 60)
                 second (gen/choose 0 60)]
                (let [d (goog.date.DateTime. (js/Date. year month day hour minute second ))]
                 (= d
                  (t/parse-local-date-time (str d))))))


(defspec ^:kaocha/skip test-localdate->string->localdate
  100
  (prop/for-all [year (gen/choose 2000 2040)
                 month (gen/choose 1 12)
                 day (gen/choose 1 28)]
                (let [d (t/local-date year month day )]
                  (= d
                     (t/parse-local-date (str d))))))

#_(gen/sample (gen/choose 0 24))

(deftest ^:kaocha/skip test-parse-invalid-local-time
  (testing "Verify invalid times are errors"
    (is (thrown? (t/parse-local-time "a4:00:00")))
    (is (t/parse-local-time "27:00:00"))
    )
  )
