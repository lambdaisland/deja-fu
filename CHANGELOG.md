# Unreleased

## Changed

- `distance-in-words` now renders approximate weeks; month ranges were adjusted

# 1.3.51 (2022-10-18 / 2d9d7f4)

## Added

- add `distance-in-words`, to render relative time strings like "3 months ago"
- add `leap-year?`

# 0.3.33 (2021-09-29 / 2f982ae)

## Changed

- BREAKING: `assoc` with a keyword that isn't understood now throws an `ex-info`

# 0.0.29 (2021-09-28 / df06a41)

## Fixed

- Support parsing dates with single digit month/day, like 2021-7-5 
- Stop relying on a private goog.date function, reimplement it ourselves instead

# 0.0.26 (2021-09-27 / e2ad16f)

## Added

- First public release of the API

## Fixed

- `format` now better handles nanoseconds of `#time/time` types
- Default format strings use the correct pattern for years (`yyyy` instead of `YYYY`)

## Changed

- `add-interval` now takes a map instead of positional arguments
