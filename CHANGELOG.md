# 0.0.23 (2021-09-27 / 39703d9)

## Added

- First public release of the API

## Fixed

- `format` now better handles nanoseconds of `#time/time` types
- Default format strings use the correct pattern for years (`yyyy` instead of `YYYY`)

## Changed

- `add-interval` now takes a map instead of positional arguments