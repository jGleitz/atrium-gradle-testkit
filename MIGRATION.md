# Migrating from 1.x to 2.0.0

2.0.0 replaces the 1.0.1 API without compatibility shims, so old names fail to compile instead of producing
deprecation warnings. Most test suites only need the dependency change, the import changes and the renames below.
[CHANGELOG.md](./CHANGELOG.md) summarizes the release.

## 1. Requirements

| | 1.0.1 | 2.0.0 |
| --- | --- | --- |
| Java running your tests | 8 | 21 or newer (bytecode targets Java 21) |
| Kotlin compiler in your build | 1.4 | 2.2 or newer (language and API version 2.2) |
| Atrium | 0.15.0 | 1.3.0 (`atrium-core`, `atrium-logic`) |
| Gradle TestKit | declared by you | declared by you, unchanged |

Your own build can use any Gradle version that runs your plugin tests on Java 21 or newer. 2.0.0 is tested with the
TestKit of Gradle 8.14.6 and 9.9.0, running builds on the same Gradle versions. Other 8.x and 9.x versions aren't
tested, so treat them as likely to work but unverified. The Gradle version that builds this repository itself (see
[README.md](./README.md#compatibility)) doesn't restrict yours.

The `gradleTestKit()` dependency of Gradle 9 adds the Kotlin standard library that ships with Gradle to your test
compile classpath. With Gradle 9.8.2 that's Kotlin 2.4, so your Kotlin compiler has to be able to read that version.

## 2. Dependencies

Replace the three 1.x artifacts with one. As in 1.x, you declare TestKit yourself, because it belongs to the Gradle
distribution that runs your tests. Neither version publishes it as a dependency:

```kotlin
dependencies {
	testImplementation(gradleTestKit())
	testImplementation("de.joshuagleitze:atrium-gradle-testkit:2.0.0")
	testImplementation("ch.tutteli.atrium:atrium-fluent:1.3.0")
}
```

| 1.x coordinate | 2.0.0 |
| --- | --- |
| `de.joshuagleitze:atrium-gradle-testkit-fluent-en` | `de.joshuagleitze:atrium-gradle-testkit` |
| `de.joshuagleitze:atrium-gradle-testkit-logic` | Removed: merged into `atrium-gradle-testkit` as internal code |
| `de.joshuagleitze:atrium-gradle-testkit-translation-en` | Removed: descriptions are plain strings, see [section 5](#5-removed-public-helpers) |
| `ch.tutteli.atrium:atrium-fluent-en_GB` (Atrium 0.15) | `ch.tutteli.atrium:atrium-fluent` 1.3.0, Atrium dropped `-en_GB` from artifact names in 1.0 |

Atrium's own API also changed between 0.15 and 1.3 (for example `toBe` became `toEqual` and `contains` became
`toContain`). Its [release notes](https://github.com/robstoll/atrium/releases) list those renames.

## 3. Renamed functions

The package `de.joshuagleitze.test.gradle` didn't change, so only imports of renamed functions need an update.

| 1.x | 2.0.0 |
| --- | --- |
| `Expect<BuildResult>.output` | `Expect<T>.output`, unchanged name |
| `Expect<BuildResult>.output { }` | `Expect<T>.output { }`, unchanged name |
| `Expect<BuildResult>.task(path)` | `Expect<T>.task(path)`, unchanged name |
| not available | `Expect<T>.task(path) { }` |
| `wasInvoked()` | `toBeInvoked()` |
| `wasInvoked { }` | `toBeInvoked { }` |
| `wasNotInvoked()` | `notToBeInvoked()` |
| not available | `toHaveOutcome(TaskOutcome)` |
| `wasSuccessful()` | `toBeSuccessful()` |
| `failed()` | `toHaveFailed()` |
| `wasUpToDate()` | `toBeUpToDate()` |
| `wasSkipped()` | `toBeSkipped()` |
| `usedCachedResult()` | `toHaveBeenLoadedFromCache()` |
| `hadNoSource()` | `toHaveNoSource()` |

A typical 1.x test:

```kotlin
expect(result) {
	task(":compileKotlin").wasSuccessful()
	task(":classes").wasInvoked()
	task(":publish").wasNotInvoked()
	output.contains("Hello World!")
}
```

becomes:

```kotlin
expect(result) {
	task(":compileKotlin").toBeSuccessful()
	task(":classes").toBeInvoked()
	task(":publish").notToBeInvoked()
	output.toContain("Hello World!")
}
```

## 4. Signature and narrowing changes

- **Generic receivers.** `output` and `task` accept any `Expect<T>` with `T : BuildResult`. Task expectations accept
  `Expect<T>` with `T : BuildTask?`, so they also work on a non-null `Expect<BuildTask>`.
- **Narrowing keeps your type.** `toBeInvoked()`, `toHaveOutcome(...)` and the outcome conveniences return
  `Expect<T & Any>` instead of `Expect<BuildTask>`. Code that stored the result as `Expect<BuildTask>` still compiles
  when `T` is `BuildTask?`.
- **Creator overloads return the original subject.** `toBeInvoked { }` returns `Expect<T>`, and the new
  `task(path) { }` returns the `BuildResult` expectation, so you can chain several tasks:

  ```kotlin
  expect(result)
  	.task(":compileKotlin") { toBeSuccessful() }
  	.task(":publish") { notToBeInvoked() }
  ```

- **Any outcome.** `toHaveOutcome(TaskOutcome.SUCCESS)` is equivalent to `toBeSuccessful()`, and it also covers outcomes
  without a convenience function.
- **JVM class names.** The functions now live in `BuildResultFeatureExtractorsKt` and `BuildTaskExpectationsKt` instead
  of `BuildResultAssertionsEnKt` and `BuildTaskAssertionsEnKt`. That only matters if you call them from Java or by
  reflection, and code compiled against 1.x has to be recompiled.

## 5. Removed public helpers

These 1.x declarations have no public replacement. They were implementation details that leaked because the 1.x
modules didn't use Kotlin's explicit API mode.

| 1.x declaration | Why it's gone | What to use |
| --- | --- | --- |
| `de.joshuagleitze.test.gradle.logic.output()`, `task(path)`, `wasInvoked()`, `wasNotInvoked()`, `outcome()`, `hadOutcome(outcome)` on `AssertionContainer` | Atrium is replacing its logic layer in a later major, so publishing these would tie this library to it | The `Expect` functions from section 3. For your own outcome checks, use `toHaveOutcome` |
| `de.joshuagleitze.test.gradle.translation.en.BuildResultAssertions` and `BuildTaskAssertions` enums | Atrium deprecates translations and will drop them, so descriptions are internal strings now | Nothing. Reports still show `output`, `task '<path>'`, `was` / `was not` `invoked` and `outcome` |

## 6. Failure reports

The descriptions in failure reports didn't change. The surrounding layout follows Atrium 1.3: reports start with
"I expected subject", and an empty creator lambda, such as `output { }`, fails with "at least one expectation defined".
If you assert on the text of failure messages, adjust those assertions to the new layout. Nothing else changed.

## 7. Build and publication

This only matters to contributors. The 1.x build used one root script with Spek 2, Dokka 1.4 and the Nexus plugins. 2.0.0 uses
convention plugins in `build-logic`, Kotest, Dokka 2 and the Vanniktech Maven publish plugin, and it checks its
public ABI against `library/api/library.api`.
