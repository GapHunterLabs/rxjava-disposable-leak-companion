<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# RxJava Disposable Leak Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- Warning icon on an RxJava subscribe(...) call whose returned
  Disposable is discarded as a bare expression statement -- there is
  no way to dispose of that subscription later, a well-documented
  source of memory/resource leaks.
- 100% static text/PSI analysis, Java and Kotlin, no network calls,
  no telemetry. Free.

[Unreleased]: https://github.com/GapHunterLabs/rxjava-disposable-leak-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/rxjava-disposable-leak-companion/commits/0.1.0
