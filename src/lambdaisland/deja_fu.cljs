(ns lambdaisland.deja-fu
  "Time/Date types for use on the frontend. This provides the following
  equivalents:

  | #time/date \"2020-10-10\"                      | java.time.LocalDate     | goog.date.Date                  |
  | #time/time \"05:30:45\"                        | java.time.LocalTime     | ductile.time.js-types/LocalTime |
  | #time/date-time \"2020-10-07T12:16:41.761088\" | java.time.LocalDateTime | goog.date.DateTime              |

  For all of these we make sure the tagged reader literals are read correctly,
  and that values of these types print with these reader tags.

  All times are \"local\", they don't carry timezone information.

  We also implement an ad-hoc API here, adding things as we need them, making
  sure that these functions hide the details of these types. All date/time
  manipulation on the frontend should go through this namespace. Avoid calling
  properties/methods of concrete types directly, and avoid using js/Date.

  Create a new local-date-time using the same syntax from goog.Date or js/Date:
    (local-date-time 2021 4 19 15 39 0 0)

  Parse a time string:
    (parse-local-date-time \"2021-01-01T23:59\")

  Get the current time:
    (local-date-time)

  Adjust the seconds component:
    (assoc (local-date-time) :seconds 0)

  Get the next day:
    (add-interval (local-date-time) {:hours 1})

  Works well with threading:
    (-> (parse-local-date-time \"2021-01-01T23:59\")
     (assoc :seconds 0)
     (to-local-date))"
  (:require [clojure.string :as str]
            [goog.date :as gdate]
            [goog.string :as gstr]
            [goog.string.format]
            [lambdaisland.data-printers :as data-printers])
  (:import (goog.date Date DateTime Interval)
           (goog.i18n DateTimeFormat)))

(def formatter
  (memoize
   (fn [pattern]
     (DateTimeFormat. pattern))))

(defn- format* [obj pattern]
  (.format (formatter pattern) obj false))

(defprotocol Format
  (format [obj] [obj format-str]
    "Symbol   Meaning                    Presentation       Example
------   -------                    ------------       -------
G#       era designator             (Text)             AD
y#       year                       (Number)           1996
Y        year (week of year)        (Number)           1997
u*       extended year              (Number)           4601
Q#       quarter                    (Text)             Q3 & 3rd quarter
M        month in year              (Text & Number)    July & 07
L        month in year (standalone) (Text & Number)    July & 07
d        day in month               (Number)           10
h        hour in am/pm (1~12)       (Number)           12
H        hour in day (0~23)         (Number)           0
m        minute in hour             (Number)           30
s        second in minute           (Number)           55
S        fractional second          (Number)           978
E#       day of week                (Text)             Tue & Tuesday
e*       day of week (local 1~7)    (Number)           2
c#       day of week (standalone)   (Text & Number)    2 & Tues & Tuesday & T
D*       day in year                (Number)           189
F*       day of week in month       (Number)           2 (2nd Wed in July)
w        week in year               (Number)           27
W*       week in month              (Number)           2
a        am/pm marker               (Text)             PM
k        hour in day (1~24)         (Number)           24
K        hour in am/pm (0~11)       (Number)           0
z        time zone                  (Text)             Pacific Standard Time
Z#       time zone (RFC 822)        (Number)           -0800
v#       time zone (generic)        (Text)             America/Los_Angeles
V#       time zone                  (Text)             Los Angeles Time
g*       Julian day                 (Number)           2451334
A*       milliseconds in day        (Number)           69540000
'        escape for text            (Delimiter)        'Date='
''       single quote               (Literal)          'o''clock'

Item marked with '*' are not supported yet.
Item marked with '#' works different than java

The count of pattern letters determine the format.
- (Text) 4 or more, use full form, <4, use short or abbreviated form if it
exists. (e.g., \"EEEE\" produces \"Monday\", \"EEE\" produces \"Mon\")

- (Number) the minimum number of digits. Shorter numbers are zero-padded to
this amount (e.g. if \"m\" produces \"6\", \"mm\" produces \"06\"). Year is handled
specially; that is, if the count of 'y' is 2, the Year will be truncated to
2 digits. (e.g., if \"yyyy\" produces \"1997\", \"yy\" produces \"97\".) Unlike other
fields, fractional seconds are padded on the right with zero.

- (Text & Number) 3 or over, use text, otherwise use number. (e.g., \"M\"
produces \"1\", \"MM\" produces \"01\", \"MMM\" produces \"Jan\", and \"MMMM\" produces
\"January\".)

Any characters in the pattern that are not in the ranges of ['a'..'z'] and
['A'..'Z'] will be treated as quoted text. For instance, characters like ':',
'.', ' ', '#' and '@' will appear in the resulting time text even if they are
not inside single quotes."))

(defprotocol Conversions
  (epoch-ms [obj] "Milliseconds since January 1, 1970")
  (to-local-date [obj] "Date part of a date or date-time")
  (to-local-time [obj] "Time part of a date-time or local time")
  (add-interval [obj values] "Add an interval to the date/time")
  (with-date [obj date] "Set the date part of a DateTime")
  (with-time [obj time] "Set the time part of a DateTime"))

(declare ->LocalTime local-date-time)

(deftype LocalTime [hours minutes seconds nanos]
  Format
  (format [obj]
    (if (and nanos (not= 0 nanos))
      (let [n (gstr/format "%09d" nanos)
            precision (loop [p 8] (if (= "0" (.charAt n p)) (recur (dec p)) (inc p)))]
        (str (format* obj "HH:mm:ss.") (subs n 0 precision)))
      (format* obj "HH:mm:ss")))
  (format [obj fmt]
    (format* obj fmt))

  Conversions
  (to-local-time [obj] obj)
  (with-date [obj date]
    (local-date-time (:year date) (:month date) (:day date) hours minutes seconds nanos))
  (add-interval [^js obj {h :hours mi :minutes s :seconds
                          :or {h 0 mi 0 s 0}}]
    (let [seconds (+ seconds s)
          minutes (+ minutes mi (quot seconds 60))
          hours   (+ hours h (quot minutes 60))
          seconds (mod seconds 60)
          minutes (mod minutes 60)]
      (->LocalTime hours minutes seconds nanos)))
  (epoch-ms [obj]
    ;; bit weird to have epoch-ms here, but it helps to support other operations
    ;; that rely on it
    (+ (* 1000 (+ seconds (* 60 (+ (* 60 hours) minutes))))
       (/ nanos 1e6)))

  Object
  (toString [obj]
    (format obj))
  (valueOf [obj]
    (+ (* (+ seconds (* (+ minutes (* hours 60)) 60)) 10e9) (or nanos 0)))
  (getHours [_] hours)
  (getMinutes [_] minutes)
  (getSeconds [_] seconds)
  (getMilliseconds [_] (/ nanos 1e6))

  IEquiv
  (-equiv [this that]
    (and (instance? LocalTime that)
         (= (.valueOf this) (.valueOf that))))
  IComparable
  (-compare [this that]
    (let [v1 (.valueOf this)
          v2 (.valueOf that)]
      (- v1 v2)))
  ILookup
  (-lookup [o k]
    (case k :hours hours :minutes minutes :seconds seconds :millis (long (/ nanos 1e6)) :nanos nanos nil))
  (-lookup [o k not-found]
    (case k :hours hours :minutes minutes :seconds seconds :millis (long (/ nanos 1e6)) :nanos nanos not-found))
  IAssociative
  (-contains-key? [coll k]
    (#{:hours :minutes :seconds :nanos :millis} k))
  (-assoc [o k v]
    (if (#{:hours :minutes :seconds :nanos :millis} k)
      (->LocalTime (if (= k :hours) v hours)
                   (if (= k :minutes) v minutes)
                   (if (= k :seconds) v seconds)
                   (cond
                     (= k :nanos)
                     v
                     (= k :millis)
                     (* v 1e6)
                     :else
                     nanos))
      (throw (ex-info (str "No field " k " in LocalTime. Valid fields: "
                           [:hours :minutes :seconds :nanos :millis])
                      {:o o :k k :v v}))))
  ISeqable
  (-seq [coll]
    (for [k [:hours :minutes :seconds :nanos :millis]]
      (MapEntry. k (get coll k) nil))))

(extend-type goog.date.Date
  Format
  (format
    ([obj]
     (format* obj "yyyy-MM-dd"))
    ([obj fmt]
     (format* obj fmt)))

  Conversions
  (epoch-ms [obj]
    (.getTime obj))
  (add-interval [^js obj {:keys [years months days hours minutes seconds]
                          :or {years 0 months 0 days 0 hours 0 minutes 0 seconds 0}}]
    (doto (.clone obj)
      (.add (goog.date.Interval. years months days hours minutes seconds))))
  (to-local-date [obj]
    obj)
  (with-time [{:keys [year month day]} {:keys [hours minutes seconds nanos]}]
    (local-date-time year month day hours minutes seconds nanos))

  IPrintWithWriter
  (-pr-writer [obj writer _opts]
    (-write writer (str "#time/date " (pr-str (format obj)))))
  Object
  (toString [obj]
    (format obj))
  IEquiv
  (-equiv [this that]
    (and (instance? goog.date.Date that)
         (= (str this) (str that))))
  IComparable
  (-compare [this that]
    (let [v1 (.valueOf this)
          v2 (.valueOf that)]
      (- v1 v2)))
  ILookup
  (-lookup
    ([o k]
     (case k
       :year (.getFullYear o)
       :month (inc (.getMonth o))
       :day (.getDate o)
       nil))
    ([o k not-found]
     (case k
       :year (.getFullYear o)
       :month (inc (.getMonth o))
       :day (.getDate o)
       not-found)))
  IAssociative
  (-contains-key? [_ k]
    (#{:year :month :day} k))
  (-assoc [o k v]
    (if (#{:year :month :day} k)
      (let [d (.clone o)]
        (case k
          :year (.setYear d v)
          :month (.setMonth d (dec v))
          :day (.setDate d v))
        d)
      (throw (ex-info (str "No field " k " in Date. Valid fields: "
                           [:year :month :day])
                      {:o o :k k :v v}))))
  ISeqable
  (-seq [coll]
    (for [k [:year :month :day]]
      (MapEntry. k (get coll k) nil))))

(defn local-date
  "contstructs a goog.date.Date

  - no arguments: returns the date for today
  - multiple arguments: takes year,month,day"
  ([]
   (goog.date.Date.))
  ([year month day]
   (goog.date.Date. year (dec month) day)))

(defn local-time
  "constructs a LocalTime
  - no arguments: get the current time
  - multiple arguments: takes hours,minutes,seconds,nanos"
  ([]
   (to-local-time (local-date-time)))
  ([hours & args]
   (let [[minutes seconds nanos] args]
     (->LocalTime (or hours 0) (or minutes 0) (or seconds 0) (or nanos 0)))))

(extend-type goog.date.DateTime
  Format
  (format
    ([obj]
     (let [fmt (cond-> "yyyy-MM-dd'T'HH:mm"
                 (or (not= 0 (.getSeconds obj))
                     (not= 0 (.getMilliseconds obj)))
                 (str ":ss")
                 (not= 0 (.getMilliseconds obj))
                 (str ".SSS"))]
       (format* obj fmt)))
    ([obj fmt]
     (format* obj fmt)))

  Conversions
  (epoch-ms [obj]
    (.getTime obj))
  (to-local-date [obj]
    (local-date (.getFullYear obj)
                (inc (.getMonth obj))
                (.getDate obj)))
  (to-local-time [obj]
    (local-time (.getHours obj)
                (.getMinutes obj)
                (.getSeconds obj)
                (* (.getMilliseconds obj) 1e6)))
  (add-interval [^js obj {:keys [years months days hours minutes seconds millis nanos]
                          :or {years 0 months 0 days 0 hours 0 minutes 0 seconds 0}}]
    (cond-> (doto (.clone obj)
              (.add (goog.date.Interval. years months days hours minutes seconds))) ; Interval only has second-precision
      (or millis nanos)
      (update :millis + (or millis 0) (long (/ (or nanos 0) 1e6)))))
  (with-date [obj local-date]
    (doto (.clone obj)
      (.setYear (.getYear local-date))
      (.setMonth (.getMonth local-date))
      (.setDate (.getDate local-date))))
  (with-time [obj local-time]
    (doto (.clone obj)
      (.setHours (:hours local-time))
      (.setMinutes (:minutes local-time))
      (.setSeconds (:seconds local-time))
      (.setMilliseconds (:millis local-time))))
  Object
  (toString [obj]
    (format obj))
  IEquiv
  (-equiv [this that]
    (and (instance? goog.date.DateTime that)
         (= (str this) (str that))))
  IComparable
  (-compare [this that]
    (let [v1 (.valueOf this)
          v2 (.valueOf that)]
      (- v1 v2)))
  ILookup
  (-lookup
    ([o k]
     (case k
       :year (.getFullYear o)
       :month (inc (.getMonth o))
       :day (.getDate o)
       :hours (.getHours o)
       :minutes (.getMinutes o)
       :seconds (.getSeconds o)
       :millis (.getMilliseconds o)
       :nanos (* (.getMilliseconds o) 1e6)
       nil))
    ([o k not-found]
     (case k
       :year (.getFullYear o)
       :month (inc (.getMonth o))
       :day (.getDate o)
       :hours (.getHours o)
       :minutes (.getMinutes o)
       :seconds (.getSeconds o)
       :millis (.getMilliseconds o)
       :nanos (* (.getMilliseconds o) 1e6)
       not-found)))
  IAssociative
  (-contains-key? [_ k]
    (#{:year :month :day :hours :minutes :seconds :millis :nanos} k))
  (-assoc [o k v]
    (if (#{:year :month :day :hours :minutes :seconds :millis :nanos} k)
      (let [d (.clone o)]
        (case k
          :year (.setYear d v)
          :month (.setMonth d (dec v))
          :day (.setDate d v)
          :hours (.setHours d v)
          :minutes (.setMinutes d v)
          :seconds (.setSeconds d v)
          :millis (.setMilliseconds d v)
          :nanos (.setMilliseconds d (long (/ v 1e6))))
        d)
      (throw (ex-info (str "No field " k " in DateTime. Valid fields: "
                           [:year :month :day :hours :minutes :seconds :millis :nanos])
                      {:o o :k k :v v}))))
  ISeqable
  (-seq [coll]
    (for [k [:year :month :day :year :month :day :hours :minutes :seconds :millis :nanos]]
      (MapEntry. k (get coll k) nil))))

;; The way goog.date.Date/DateTime are defined means the constructor function is
;; anonymous, it's name is "", and this property is by default read-only. We
;; make it writable and set it, so that Puget can inspect it.
(js/Object.defineProperty goog.date.Date "name" #js {:writable true})
(js/Object.defineProperty goog.date.DateTime "name" #js {:writable true})
(set! (.-name goog.date.Date) "goog.date.Date")
(set! (.-name goog.date.DateTime) "goog.date.DateTime")

(data-printers/register-print LocalTime 'time/time (comp str format))
(data-printers/register-pprint LocalTime 'time/time (comp str format))
(data-printers/register-print goog.date.Date 'time/date (comp str format))
(data-printers/register-pprint goog.date.Date 'time/date (comp str format))
(data-printers/register-print goog.date.DateTime 'time/date-time (comp str format))
(data-printers/register-pprint goog.date.DateTime 'time/date-time (comp str format))

(defn local-date-time
  "contstructs a goog.date.DateTime

  - no arguments: get the current date and time
  - single argument: convert UNIX timestamp (milliseconds) to a DateTime
  - multiple arguments: takes year,month,day,hours,minutes,seconds,nanos.
    Seconds and nanos are optional."
  ([]
   (goog.date.DateTime.))
  ([epoch-ms]
   (assert (number? epoch-ms))
   (goog.date.DateTime/fromTimestamp epoch-ms))
  ([year month day hours minutes]
   (local-date-time year month day hours minutes 0 0))
  ([year month day hours minutes seconds]
   (local-date-time year month day hours minutes seconds 0))
  ([year month day hours minutes seconds nanos]
   (goog.date.DateTime. year (dec month) day hours minutes seconds (long (/ nanos 1e6)))))

(def date-string-regex
  #"^(\d{4})(?:(?:-?(\d{1,2})(?:-?(\d{1,2}))?)|(?:-?(\d{3}))|(?:-?W(\d{2})(?:-?([1-7]))?))?$")

(defn- setIso8601DateOnly
  "like the goog.date function, but reimplemented because theirs is private, and
  to support single digits months and days."
  [d yyyy-mm-dd]
  (let [[_ year month date dayOfYear week dayOfWeek]
        (re-find date-string-regex yyyy-mm-dd)
        dayOfWeek (or dayOfWeek 1)]
    (.setFullYear d year)
    (cond
      dayOfYear
      (do
        (.setDate d 1)
        (.setMonth d 0)
        (.add d (goog.date.Interval. goog.date.Interval/DAYS (- dayOfYear 1))))

      week
      (do
        (.setMonth d 0)
        (.setDate d 1)
        (let [jsDay (.getDay d)
              jan1WeekDay (or jsDay 7)
              THURSDAY 4
              startDelta (if (<= jan1WeekDay THURSDAY)
                           (- 1 jan1WeekDay)
                           (- 8 jan1WeekDay))
              absoluteDays (+ (js/Number. dayOfWeek) (* 7 (dec (js/Number week))))
              delta (+ startDelta absoluteDays -1)]
          (.add d (goog.date.Interval. goog.date.Interval/DAYS delta))))

      :else
      (do
        (when month
          (.setDate d 1)
          (.setMonth d (dec month)))
        (when date
          (.setDate d date)))))
  d)

(defn parse-local-date
  "Parse a date (YYYY-MM-DD) to a goog.date.Date

  Reimplementation of goog.date.Date.fromIsostring but with support for single
  digit month/day."
  [yyyy-mm-dd]
  (let [ret (Date. 2000)]
    (when (setIso8601DateOnly ret yyyy-mm-dd)
      ret)))

(defn parse-local-time
  "Parse a timestamp (HH:MM:SS or HH:MM:SS.mmm or HH:MM:SS.nnnnnn) to a LocalTime."
  [time]
  (let [time (if (= ":" (second time)) (str "0" time) time)]
    (let [hours   (js/parseInt (subs time 0 2) 10)
          minutes (js/parseInt (subs time 3 5) 10)
          seconds (if (> (count time) 5)
                    (js/parseInt (subs time 6 8) 10)
                    0)
          nanos   (when (= \. (.charAt time 8))
                    (subs time 9))
          nanos   (if nanos
                    (* (js/parseInt nanos 10) (Math/pow 10 (- 9 (count nanos))))
                    0)]
      (->LocalTime hours minutes seconds nanos))))

(defn parse-local-date-time
  "Parse an ISO timestamp without time zone information to a goog.date.DateTime.
  We implement our own parsing logic for the time section, to prevent time zone
  shifting. The numbers you provide in the timestamp are the numbers that end up
  in the DateTime. If we leave it to goog.date then it will assume the timestamp
  is UTC, and shift it to the browser's timezone."
  [date-time]
  (let [d           (goog.date.DateTime. 2000)
        delim       (if (= -1 (.indexOf date-time "T")) " " "T")
        [date time] (str/split date-time delim)
        d           (when (setIso8601DateOnly d date) d)
        ^js t       (parse-local-time time)]
    d
    (when d
      (.setHours d (.-hours t))
      (.setMinutes d (.-minutes t))
      (.setSeconds d (.-seconds t))
      ;; goog.date.DateTime only has milliseconds, so we truncate. Not a huge
      ;; loss.
      (.setMilliseconds d (long (/ (.-nanos t) 1e6)))
      d)))

(defn parse-offset-date-time
  "Parse an ISO timestamp with timezone offset (Z or +/-) to a goog.date.DateTime.
  Note that the result is essentially a LocalDateTime, since none of the types
  at our disposal on the frontend actually store a timezone or offset. Instead
  goog.date's parsing logic will shift the time based on the difference between
  the time zone in the timestamp, and the timezone the browser is in."
  [date-time]
  (goog.date.DateTime/fromIsoString date-time))

(defn current-time-millis
  "Get the current UNIX timestamp in milliseconds."
  []
  (js/Date.now))

(defn today
  "Get a #time/date with the current date, convenience API alternative to
  calling `(local-date)` without args."
  []
  (goog.date.Date.))

(defn millis-between
  "Get the interval between two date/time objects in milliseconds"
  [begin end]
  (let [begin-ms (epoch-ms begin)
        end-ms (epoch-ms end)
        millis (js/Math.ceil (- end-ms begin-ms))]
    (cond
      (< millis 0) (js/Math.floor millis)
      (< 0 millis) (js/Math.ceil millis)
      :else 0)))

(defn minutes-between
  "Get the interval between two date/time objects in minutes"
  [begin end]
  (Math/floor (/ (millis-between begin end) 1000 60)))

(defn days-between
  "Get the interval between two date/time objects in days"
  [begin end]
  (Math/floor (/ (millis-between begin end) 1000 60 60 24)))

(defn browser-timezone-offset
  "Get the difference, in minutes, between browser's time zone and UTC.
  e.g. when in CET (+1) this returns -60."
  []
  (.getTimezoneOffset (js/Date.)))
