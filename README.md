# deja-fu

<!-- badges -->
[![cljdoc badge](https://cljdoc.org/badge/com.lambdaisland/deja-fu)](https://cljdoc.org/d/com.lambdaisland/deja-fu) [![Clojars Project](https://img.shields.io/clojars/v/com.lambdaisland/deja-fu.svg)](https://clojars.org/com.lambdaisland/deja-fu)
<!-- /badges -->

Lightweight ClojureScript local time/date library

While this library is relatively new, it has a battery of tests and has been used in production code.

## Features

<!-- installation -->
## Installation

To use the latest release, add the following to your `deps.edn` ([Clojure CLI](https://clojure.org/guides/deps_and_cli))

```
com.lambdaisland/deja-fu {:mvn/version "0.0.0"}
```

or add the following to your `project.clj` ([Leiningen](https://leiningen.org/))

```
[com.lambdaisland/deja-fu "0.0.0"]
```
<!-- /installation -->

## Rationale

While ClojureScript doesn't have the [chaos of Clojure's DateTime](https://lambdaisland.com/blog/2017-07-26-dates-in-clojure-making-sense-of-the-mess), JavaScript lacks

## Usage

Create a new local-date-time using the same syntax from goog.Date or js/Date:

```clojure
    (local-date-time 2021 4 19 15 39 0 0)
```

Parse a time string:
```clojure
    (parse-local-date-time "2021-01-01T23:59")
```

Get the current time:
```clojure
    (local-date-time)
```

Adjust the seconds component:
```clojure
    (assoc (local-date-time) :seconds 0)
```

Get the next day:
```clojure
    (add-interval (local-date-time) 0 0 1)
```

Works well with threading:
```clojure
    (-> (parse-local-date-time "2021-01-01T23:59")
     (assoc :seconds 0)
     (to-local-date))
```

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

Available under the terms of the Eclipse Public License 1.0, see LICENSE.txt
<!-- /license -->
