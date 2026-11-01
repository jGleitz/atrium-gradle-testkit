# Changelog

This file is the migration record from 1.x to 2.0.0. It isn't a continuously generated log: release notes for every
version after 2.0.0 live in [GitHub Releases](https://github.com/jGleitz/atrium-gradle-testkit/releases).

## 2.0.0

2.0.0 is a breaking major release. It replaces the 1.0.1 API outright, without deprecated aliases or compatibility
artifacts. [MIGRATION.md](./MIGRATION.md) has the step-by-step upgrade path and an old-to-new name table.

### Breaking changes

- **One artifact.** `de.joshuagleitze:atrium-gradle-testkit` replaces `atrium-gradle-testkit-fluent-en`,
  `atrium-gradle-testkit-logic` and `atrium-gradle-testkit-translation-en`. The Kotlin package stays
  `de.joshuagleitze.test.gradle`.
- **Atrium 1.3.** The library builds on Atrium 1.3.0 (`atrium-core` and `atrium-logic`) instead of 0.15.0. Use
  `ch.tutteli.atrium:atrium-fluent:1.3.0` (or a later 1.x) in your tests.
- **Renamed expectations** following Atrium's `to + infinitive` scheme: `wasInvoked` is now `toBeInvoked`,
  `wasNotInvoked` is `notToBeInvoked`, `wasSuccessful` is `toBeSuccessful`, `failed` is `toHaveFailed`, `wasUpToDate`
  is `toBeUpToDate`, `wasSkipped` is `toBeSkipped`, `usedCachedResult` is `toHaveBeenLoadedFromCache` and `hadNoSource`
  is `toHaveNoSource`.
- **Removed public helpers.** The `AssertionContainer` functions in `de.joshuagleitze.test.gradle.logic` and the
  translation enums in `de.joshuagleitze.test.gradle.translation.en` are gone. They're internal now.
- **TestKit isn't a transitive dependency.** Declare `testImplementation(gradleTestKit())` yourself.
- **Floors raised:** Java 21 (was Java 8 bytecode) and Kotlin 2.2 (was 1.4).
- **Binary incompatible:** the public functions moved from the `BuildResultAssertionsEnKt` and
  `BuildTaskAssertionsEnKt` classes to `BuildResultFeatureExtractorsKt` and `BuildTaskExpectationsKt`, and all
  receivers are generic.

### New

- `toHaveOutcome(TaskOutcome)` checks any outcome, including ones without a convenience function.
- `task(path) { ... }` collects expectations about a task and returns the original `BuildResult` expectation.
- Generic receivers: `output` and `task` work on any `Expect<T : BuildResult>`. Task expectations work on
  `Expect<T : BuildTask?>`, and narrowing keeps the subtype as `Expect<T & Any>`.
- API documentation with runnable samples for every function.

### Unchanged

- Failure reports keep the descriptions `output`, `task '<path>'`, `was` / `was not` `invoked` and `outcome`. Layout and
  wording around them follow Atrium 1.3.
- A task that the build didn't run is still a `null` subject, so `task(":x").toEqual(null)` and `notToBeInvoked()`
  behave as before.
