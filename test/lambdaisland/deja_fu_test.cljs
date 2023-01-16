(ns lambdaisland.deja-fu-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [clojure.test.check.clojure-test :refer [defspec]]
            [lambdaisland.deja-fu :as fu]
            [goog.date.DateTime]))

(deftest test-days-between
  (testing "Vaidate 'For example, 11:59 p.m. 1/1/21 and 12:01 a.m. 1/2/21 are 0 days apart.'"
    (is (= 0.0
           (fu/days-between
            (goog.date.DateTime. (js/Date. 2021 0 1 23 59 ))
            (goog.date.DateTime. (js/Date. 2021 0 2 0 1))))))
  (testing "Two consecutive days return 1 if those days are"
    (testing "ordinary"
      (is (= 1 (fu/days-between (fu/local-date 2021 1 1) (fu/local-date 2021 1 2)))))
    (testing "around daylight savings"
      (is (= 1 (fu/days-between (fu/local-date 2021 11 6) (fu/local-date 2021 11 7)))))
    (testing "constructed as goog.date.DateTime objects"
      (is (= 1 (fu/days-between (goog.date.DateTime. 2021 0 1) (goog.date.DateTime. 2021 0 2)))))
    (testing "constructed as js/Date. and then converted to goog.date.DateTime objects"
      (is (= 1 (fu/days-between
                (goog.date.DateTime. (js/Date. 2021 0 1))
                (goog.date.DateTime. (js/Date. 2021 0 2))))))
    (testing "one is a goog.date.DateTime and the other is a local-time."
      (is (= 1 (fu/days-between
                (goog.date.DateTime. 2021 0 1)
                (fu/local-date 2021 1 2)))))))

(deftest test-parse-local-time
  (testing "Verify that times are parsed correctly"
    (testing "when the format is HH:MM:SS"
      (is (= (fu/parse-local-time "23:01:01") (fu/->LocalTime 23 1 1 nil)))
      (is (= (fu/parse-local-time "11:01:01") (fu/->LocalTime 11 1 1 nil))))
    (testing "when the format is HH:MM:SS.mmm"
      (is (= (fu/parse-local-time "23:01:01.001") (fu/->LocalTime 23 1 1 1e6)))
      (is (= (fu/parse-local-time "11:01:01.001") (fu/->LocalTime 11 1 1 1e6))))
    (testing "when the format is HH:MM:SS.nnnnnn"
      (is (= (fu/parse-local-time "23:01:01.001000") (fu/->LocalTime 23 1 1 1e6)))
      (is (= (fu/parse-local-time "11:01:01.001000") (fu/->LocalTime 11 1 1 1e6))))))

(fu/parse-local-time (str (fu/->LocalTime 11 1 1 1e6)))

(deftest roundtrip
  (let [time-example (fu/->LocalTime 11 1 1 0 )]
    (is (= time-example (fu/parse-local-time (str time-example))))))

(deftest roundtrip-nanos
  (let [time-example (fu/->LocalTime 11 1 1 1e6 )]
    (is (= time-example (fu/parse-local-time (str time-example))))))

(defspec test-time->string->time
  100
  (prop/for-all [h (gen/choose 0 24)
                 m (gen/choose 0 60)
                 s (gen/choose 0 60)]
    (= (fu/->LocalTime h m s nil)
       (fu/parse-local-time (str (fu/->LocalTime h m s nil))))))

(defspec test-datetime->string->datetime
  100
  (prop/for-all [year  (gen/choose 2000 2040)
                 month (gen/choose 1 12)
                 day (gen/choose 1 28)
                 hour (gen/choose 0 24)
                 minute (gen/choose 0 60)
                 second (gen/choose 0 60)]
    (let [d (goog.date.DateTime. (js/Date. year month day hour minute second))]
      (= d
         (fu/parse-local-date-time (str d))))))


(defspec ^:kaocha/skip test-localdate->string->localdate
  100
  (prop/for-all [year (gen/choose 2000 2040)
                 month (gen/choose 1 12)
                 day (gen/choose 1 28)]
    (let [d (fu/local-date year month day)]
      (= d (fu/parse-local-date (str d))))))

#_(gen/sample (gen/choose 0 24))

(deftest ^:kaocha/skip test-parse-invalid-local-time
  (testing "Verify invalid times are errors"
    (is (thrown? (fu/parse-local-time "a4:00:00")))
    (is (fu/parse-local-time "27:00:00"))))

(deftest defult-format-test
  (is (= "2021-06-30" (fu/format (fu/local-date 2021 6 30))))
  (is (= "2021-06-30T10:11:12.135" (fu/format (fu/local-date-time 2021 6 30 10 11 12 135e6))))
  (is (= "10:11:12.135" (fu/format (fu/local-time 10 11 12 135e6))))
  (is (= "10:11:12.135678" (fu/format (fu/local-time 10 11 12 135678e3))))
  (is (= "10:11:12.135678123" (fu/format (fu/local-time 10 11 12 135678123)))))

(deftest parse-local-date-dest
  (is (= (fu/parse-local-date "2021-10-11") (fu/local-date 2021 10 11)))
  (is (= (fu/parse-local-date "2021-10-5") (fu/local-date 2021 10 5))))

(deftest assoc-invalid-keys
  (is (thrown? ExceptionInfo (update (fu/local-date) :days inc)))
  (is (thrown? ExceptionInfo (update (fu/local-time) :minute inc)))
  (is (thrown? ExceptionInfo (update (fu/local-date-time) :years inc))))

(deftest distance-in-words-test
  (is (= "less than 5 seconds"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2022 1 1 0 0 1))))

  (is (= "less than 10 seconds"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2022 1 1 0 0 7))))

  (is (= "half a minute"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2022 1 1 0 0 20))))

  (is (= "1 minute"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2022 1 1 0 1 20))))

  (is (= "2 minutes"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2022 1 1 0 1 50))))

  (is (= "about 1 hour"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2022 1 1 1 1 50))))

  (is (= "about 2 hours"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2022 1 1 2 1 50))))

  (is (= "1 day"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2022 1 2 2 1 50))))

  (is (= "about 1 week"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2022 1 9 0 1 50))))

  (is (= "about 2 weeks"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2022 1 15 0 0 0))))

  (is (= "1 month"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2022 2 2 2 1 50))))

  (is (= "3 months"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2022 4 2 2 1 50))))

  (is (= "about 1 year"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2023 4 2 2 1 50))))

  (is (= "over 1 year"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2023 5 2 2 1 50))))

  (is (= "almost 2 years"
         (fu/distance-in-words (fu/local-date-time 2022 1 1 0 0 0)
                               (fu/local-date-time 2023 11 2 2 1 50)))))
