# RxJava Disposable Leak Companion

Warning icon on an RxJava `xxx.subscribe(...)` call
(Observable/Flowable/Single/Maybe/Completable, RxJava 2.x/3.x) written
as a bare expression statement — the returned `Disposable` is
discarded immediately, with no way to ever call `.dispose()` on it
later. RxJava's own guidance is explicit: always dispose of
subscriptions when no longer needed, or this is a well-documented
source of memory/resource leaks.

## Why it exists

`orders.subscribe(order -> process(order));` compiles fine and starts
the subscription — but the `Disposable` it returns is thrown away on
the spot. There is no variable, no `CompositeDisposable.add(...)`,
nothing — the subscription can never be cancelled, and if the source
never completes (a long-lived stream, a UI event source, a polling
observable), it silently keeps the subscriber alive for the rest of
the application's life.

## Why built this way

- **100% static text/PSI analysis** — matches the method name and
  argument shape by simple text, so it works whether the real RxJava
  jar is on the classpath or not. Java and Kotlin.
- **Confirmed gap**: JetBrains' own bundled "Reactive Streams" plugin
  (Ultimate-only) has 12 real inspections covering Reactor, RxJava,
  and Mutiny (unused Publisher, blocking calls in non-blocking scope,
  unfinished StepVerifier, and more) — none of them cover a discarded
  `subscribe()` Disposable. Confirmed by extracting and reading the
  plugin's own `plugin.xml`, not just its documentation.

## v0.1 scope — stated honestly, not exhaustively

Only flags the lambda/callback-based `subscribe(...)` overloads (0 to
4 lambda/method-reference arguments), which are the overloads that
actually return `Disposable`. The single-argument
`subscribe(Observer)`/`subscribe(DisposableObserver)` overload returns
`void`/`Unit` (disposal is handled differently, via
`Observer.onSubscribe`) and is never flagged. Matches by simple method
name, not real type resolution — an unrelated `subscribe()` method on
some other type is a possible (rare) false positive.

## Usage

Open any Java/Kotlin file using RxJava. A `subscribe(...)` call whose
`Disposable` is discarded as a bare statement shows a warning icon.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
