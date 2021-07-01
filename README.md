# Déjà Fu

<!-- badges -->
[![cljdoc badge](https://cljdoc.org/badge/com.lambdaisland/deja-fu)](https://cljdoc.org/d/com.lambdaisland/deja-fu) [![Clojars Project](https://img.shields.io/clojars/v/com.lambdaisland/deja-fu.svg)](https://clojars.org/com.lambdaisland/deja-fu)
<!-- /badges -->

Lightweight ClojureScript local time/date library

> A martial art in which the user's limbs move in time as well as space,
> allowing them to go back in time and punch the opponent repeatedly so that the
> opponent feels the effects of past blows while standing in front of the
> apparently motionless [Déjà > Fu](https://wiki.lspace.org/mediawiki/D%C3%A9j%C3%A0_Fu) practitioner. Similar
> tricks can be used to render the opponent's attacks ineffective or otherwise
> incapacitate him.
>
> It is best described as "the feeling that you have been kicked in the head this way before".
>
> — Terry Pratchet

<!-- installation -->
## Installation

Deja-fu is still in alpha, for now you can use it as a git dependency from `deps.edn`.

<!-- /installation -->

## Rationale

This library is the result of a particular set of insights and design
constraints. The main insight is that it is valuable to have distinct types for
distinct concepts like a "calendar date" vs "wall time" vs a "date-time /
timestamp". On the JVM we have known this for some time, thanks to Joda-Time and
later the java.time API (aka JSR-310).

The equivalent in the JavaScript/ClojureScript world is js-joda, which is used
by the ClojureScript version of [juxt/tick](https://github.com/juxt/tick), or
the upcoming [TC39 Temporal API](https://github.com/tc39/proposal-temporal).
Eventually we can expect Temporal to become part of browsers and other JS
runtimes, but until then, a hefty polyfill is needed, significantly blowing up
build size. The same is true of js-joda.

For applications where dealing with time is not enough of their core business to
justify these large dependencies, a lighter alternative is needed. This
is where deja-fu comes in. It builds upon the Google Closure library's
`goog.date.Date` and `goog.date.DateTime`, and adds a third "time" type, thus
providing representations for three concepts.

- a date
- a time without time zone
- a date+time without time zone

For these it provides a limited but flexible and highly idiomatic ClojureScript
API.

Note that none of them carry time zone information. It is up to the programmer
to decide how to deal with time zones. Either make sure all times are in the
user's (browser's) time zone, or in the server's time zone, or in UTC, and
convert accordingly on the edges. Or track time+zone explicitly. We do provide a
basic API for doing time zone offset shifts, and for querying the time zone
offset of the browser running the app.

The API that deja-fu provides is by no means as comprehensive as java.time,
js-joda, or Temporal. Instead we provide basic primitives for parsing,
formatting, and manipulating times in a way that hopefully feels intuitive and
convenient for Clojure programmers. It tries to be Good Enough while retaining a
limited footprint and paving over some of the quirkiness of dealing with dates
and times in a JavaScript world.

## Getting started

We provide a single namespace, `lambdaisland.deja-fu`. All examples below call
functions from that namespace.

``` clojurescript
(ns my-ns
  (:require [lambdaisland.deja-fu :as fu]))
  
(fu/local-time)
```

For brevity we have omitted the `fu/` prefix in the rest of the README.

## Types

| Reader / printer syntax                        | deja-fu type                    | equivalent JDK type     |
|------------------------------------------------|---------------------------------|-------------------------|
| #time/date "2020-10-10"                        | goog.date.Date                  | java.time.LocalDate     |
| #time/time "05:30:45"                          | ductile.time.js-types/LocalTime | java.time.LocalTime     |
| #time/date-time "2020-10-07T12:16:41.761088"   | goog.date.DateTime              | java.time.LocalDateTime |

deja-fu includes a `data_readers.cljs` which provides support for tagged
literals in code, and it registers print and pretty-print handlers for printing
values of these types as tagged literals.

For each type there's a constructor that takes either the individual elements as
positional arguments, or no args to get the current date/time/datetime. Each has
a parse function that takes a string.

``` clojurescript
;;;;;;;;;;;;;;;;;;; Time
;; Current time
(local-time) ;; => #time/time "09:33:53.048000"

;; Hours / Minutes / Seconds / Nanos
(local-time 10 15 59 123000000) ;; => #time/time "10:15:59.123"
(local-time 10 15 59 123456789) ;; => #time/time "10:15:59.123456789"

;; Parse time
(parse-local-time "10:15:59.123456789") ;; => #time/time "10:15:59.123456789"

;;;;;;;;;;;;;;;;;;; Date
;; Current date
(local-date);; => #time/date "2021-06-30"

;; Year / Month / Day
(local-date 2021 6 30) ;; => #time/date "2021-06-30"
(parse-local-date "2021-06-30");; => #time/date "2021-06-30"

(local-date-time);; => #time/date-time "2021-06-30T09:47:09.191"

;;;;;;;;;;;;;;;;;;; DateTime
;; Year / Month / Day / Hours / Minutes / Seconds / Nanos
(local-date-time 2021 6 30 10 15 59 123456789);; => #time/date-time "2021-06-30T10:15:59.123"
;; See note on millis vs nanos

;; Seconds / Nanos are optional
(local-date-time 2021 6 30 10 15) ;; => #time/date-time "2021-06-30T10:15"

;; Parse date+time
(parse-local-date-time "2021-06-30T10:15:59.123") ;; => #time/date-time "2021-06-30T10:15:59.123"
```

## Keyword access

The killer feature of Deja-fu is that all three types implement several built-in
ClojureScript protocols, making them behave much like regular maps or records.

```clojurescript
(keys (local-time)) ;; => (:hours :minutes :seconds :nanos :millis)
(:seconds (local-time)) ;; => 16
(assoc (local-date-time) :hours 10 :minutes 20 :seconds 30) ;; => #time/date-time "2021-06-30T10:20:30.529"
(update (local-date-time) :year inc) ;; => #time/date-time "2022-06-30T10:17:24.561"

(let [{:keys [hours minutes]} (local-time)]
  (str "It is " minutes " past " hours))
;; => "It is 18 past 10"
```

You can destructure, update certain fields, etc.

## Converting between types

We provide the following conversion methods

``` clojurescript
(defprotocol Conversions
  (epoch-ms [obj] "Milliseconds since January 1, 1970")
  (to-local-date [obj] "Date part of a date or date-time")
  (to-local-time [obj] "Time part of a date-time or local time")
  (add-interval [obj values] "Add an interval to the date/time")
  (with-date [obj date] "Set the date part of a DateTime")
  (with-time [obj time] "Set the time part of a DateTime"))
```

`to-local-date` / `to-local-time` simply truncate a date-time to either the date
or the time part. When called on a `#time/date` or `#time/time` respectively
they are idempotent.

``` clojurescript
(to-local-date (local-date-time)) ;; => #time/date "2021-06-30"
(to-local-time (local-date-time)) ;; => #time/time "10:33:49.267"
(to-local-date (local-date)) ;; => #time/date "2021-06-30"
(to-local-time (local-time)) ;; => #time/time "10:35:07.546"
```

`with-date` / `with-time` combines two date/time objects, retaining the date
part of one, and the time part of the other.

``` clojurescript
(with-time (local-date) (local-time));; => #time/date-time "2021-06-30T10:33:27.691"

(with-time (local-date-time) (local-time));; => #time/date-time "2021-06-30T10:33:22.432"
```

`add-interval` takes a map with `:years` / `:months` / `:days`, etc.

```clojurescript
(add-interval (local-date) {:years 5 :days 3}) ;; => #time/date "2026-07-03"
(add-interval (local-time) {:minutes 5}) ;; => #time/time "10:42:08.239" 
```

`epoch-ms` returns a UNIX timestamp with millisecond precision, i.e., the number
of milliseconds since January 1, 1970.

## Formatting

For formatting we rely on the Google Closure library, see the docstring of
`lambdaisland.deja-fu/format` for valid patterns. Without a pattern, `format`
will use standard ISO formatting.

```clojurescript
(format (local-date-time)) ;; => "2021-06-30T10:39:18.423"
(format (local-date-time) "dd. MMMM yyyy") ;; => "30. June 2021"
```

Note that the Google Closure library contains many locale-specific patterns
under [`goog.i18n.DateTimePatterns_*`](https://google.github.io/closure-library/api/goog.i18n.DateTimePatterns.html).

``` clojurescript
(ns my-ns
  (:require [lambdaisland.deja-fu :as fu]
            [goog.i18n.DateTimePatterns_de :as DateTimePatterns_de]))
            
(fu/format date DateTimePatterns_de/MONTH_DAY_YEAR_MEDIUM)
```

## Assorted API

- `current-time-millis` Get the current UNIX timestamp
- `today` Get the current day
- `millis-between` / `minutes-between` / `days-between` Get the interval between two times/dates/date-times in milliseconds, minutes, or days.
- `browser-time-zone-offset` Get the offset between the browser's timezone and UTC

To get the current UTC time you can use

```clojurescript
(add-interval (local-time) {:minutes (browser-timezone-offset)})
```

## Caveats

### Nanoseconds vs Milliseconds

The deja-fu API generally works with nanoseconds, for instance, the constructors
above take nanoseconds. However, `goog.date.DateTime`, being based on `js/Date`,
only has millisecond precision. This means nanosecond values are truncated
to millisecond precision.

Our own `lambdaisland.deja-fu/LocalTime` type does not have this limitation; it
has full nanosecond precision.

For keyword access we provide both `:millis` and `:nanos`, as a convenience.

We may try to address this in a future version, by tacking a separate
"nanoseconds" field onto `goog.date.DateTime`. As long as you stick to deja-fu
APIs this change should be transparent, and all code should work as before,
except that values no longer get truncated. However, when using the
`goog.date.DateTime` API directly, it will not be aware of the nanosecond field,
and will continue to provide values truncated to the millisecond.

### Piggieback printing

When printing deja-fu values via nREPL and piggieback, they may come out looking
a little off.

```clojurescript
;; Expected
#time/time "10:15:59.123"

;; Actual
#time/time10:15:59.123
```

This is an issue with how Piggieback handles tagged literals it doesn't know how
to read in Clojure. If you have data-readers set up on the Clojure side for
reading time/time, time/date, and time/date-time, then this will not be an
issue. Note that you'll have to set these up yourself, since using
[time-literals](https://github.com/henryw374/time-literals) is not an option in
this case, see the next point.

### Incompatibility with other libraries

deja-fu provides its own data-readers for `#time/time`, `#time/date` and
`#time/date-time`. This means it conflicts with
[time-literals](https://github.com/henryw374/time-literals), which provides
these for both clj and cljs, but is based on js-joda. If you are using
deja-fu, you should not be using js-joda (and vice versa).

Other libraries that are incompatible because they rely on js-joda or
time-literals or both are cljs.java-time, cljc.java-time, and tick.

## References

- This
  [spreadsheet](https://docs.google.com/spreadsheets/d/1GEL16gy0cGKXlkhHtiZpAw9nbca3QorAvk4oeupBdXs/edit#gid=1884997507)
  contains an overview of time types provided by different libraries and
  platforms, and some notes on different Clojure and ClojureScript options.

<!-- opencollective -->
## Lambda Island Open Source

<img align="left" src="https://github.com/lambdaisland/open-source/raw/master/artwork/lighthouse_readme.png">

&nbsp;

deja-fu is part of a growing collection of quality Clojure libraries created and maintained
by the fine folks at [Gaiwan](https://gaiwan.co).

Pay it forward by [becoming a backer on our Open Collective](http://opencollective.com/lambda-island),
so that we may continue to enjoy a thriving Clojure ecosystem.

You can find an overview of our projects at [lambdaisland/open-source](https://github.com/lambdaisland/open-source).

&nbsp;

&nbsp;
<!-- /opencollective -->

<!-- contributing -->
## Contributing

Everyone has a right to submit patches to deja-fu, and thus become a contributor.

Contributors MUST

- adhere to the [LambdaIsland Clojure Style Guide](https://nextjournal.com/lambdaisland/clojure-style-guide)
- write patches that solve a problem. Start by stating the problem, then supply a minimal solution. `*`
- agree to license their contributions as EPL 1.0.
- not break the contract with downstream consumers. `**`
- not break the tests.

Contributors SHOULD

- update the CHANGELOG and README.
- add tests for new functionality.

If you submit a pull request that adheres to these rules, then it will almost
certainly be merged immediately. However some things may require more
consideration. If you add new dependencies, or significantly increase the API
surface, then we need to decide if these changes are in line with the project's
goals. In this case you can start by [writing a pitch](https://nextjournal.com/lambdaisland/pitch-template),
and collecting feedback on it.

`*` This goes for features too, a feature needs to solve a problem. State the problem it solves, then supply a minimal solution.

`**` As long as this project has not seen a public release (i.e. is not on Clojars)
we may still consider making breaking changes, if there is consensus that the
changes are justified.
<!-- /contributing -->

<!-- license -->
## License

Copyright &copy; 2021 Arne Brasseur and contributors

Available under the terms of the Mozilla Public License 2.0, see LICENSE.txt
<!-- /license -->
