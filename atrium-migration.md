# Migrating an Atrium 0.16 Extension Library to Atrium 1.3: Conventions, APIs and a Migration Plan

The most important finding: the Proof-based redesign (`ProofContainer`, `_core`, `_coreAppend`) did **not** ship in Atrium 1.3.0. In 1.3.0, extensions are still written with `Expect<T>` plus `_logic` (`AssertionContainer`) from `atrium-logic`, using `String` descriptions and `Text` representations. The release notes for v1.3.0 say: "We had to postpone the rewrite from Assertion to Proof initially planned for 1.3.0. This will now follow in 2.x".\[1\] So migrate to the `_logic`/`String`/`Text` style now. Keep the extension's own API thin so that the planned 2.x move (Assertion→Proof, logic merged into core, `en_GB` possibly dropped from package names) only touches a small internal layer.

## TL;DR
- **Target 1.3.0 with api-fluent + `_logic`; skip infix and translations.** `ExpectImpl` and `atrium-domain-builders` were removed in 0.17, and `atrium-core-api` was renamed to `atrium-core`. `-en_GB` was dropped from artifact names in 1.0.\[2\] `translations_de_CH` was removed in 1.3. api-infix got its last release in 1.3.0 ("We will discontinue the api-infix, the usage is too low to justify the additional work. This was the last release", per the v1.3.0 release notes) and will be dropped in 2.0.0. Replace translation enums with `String` descriptions and `Text` representations, which is also the direction Atrium announced for 2.x.
- **Follow the 1.x API conventions.** Use `to + infinitive` naming (`toEqual`, `toHaveX`, `notToX`), a type parameter `<T : X> Expect<T>`, and paired overloads: a `val`/`fun` that returns `Expect<R>`, and a `fun(assertionCreator: Expect<R>.() -> Unit): Expect<T>`. Use `feature(Class::prop)` with class references inside expectation functions (not `its`, not the `f(it::…)` lambda). Add KDoc with `@return`, `@since` and `@sample`, with samples living in test sources.
- **Testing: drop the dependency on `atrium-specs`/Spek.** Atrium itself is migrating its specs from Spek to kotlin-test, file by file (still ongoing in 1.3.0), and plans to drop the abstract spec classes.\[1\]\[3\] Write extension tests as plain kotlin-test/JUnit 5 (or Kotest) tests that run each function against passing and failing subjects. Atrium 1.3 requires Kotlin ≥ 1.4, JVM 11 and the JS IR backend.\[4\] Atrium has emitted JDK 11 bytecode since 0.18, so the extension cannot keep emitting Java 8 bytecode.\[3\]

## Key Findings

### 1. What changed between 0.16 and 1.3 (relevant to extension authors)

| Release | Change affecting extensions |
|---|---|
| 0.17.0 | Removed deprecated modules `atrium-core-robstoll`, `atrium-core-robstoll-lib` and `atrium-domain-builders` (including `ExpectBuilder`).\[3\] This is where `ExpectImpl` goes away. Removed `ReporterFactory`, `RawString` and `collectForDifferentSubject`. "we renamed core-api to core" (verified while migrating this project to 0.17.0: Maven Central has `atrium-core`, `atrium-logic` and `atrium-fluent-en_GB` at 0.17.0, but no `atrium-core-api`, `atrium-domain-builders` or `atrium-core-robstoll` at 0.17.0; the production `_logic` code of this project needed no change). The whole API was renamed to the `to + infinitive` schema; `assert`/`assertThat` verbs deprecated.\[3\]\[5\] |
| 0.18.0 | Reporting aligned to `to + infinitive`, error messages now start with "I expected...". Deprecated `Description...Assertion` (use `Description...Expectation`), `DescriptionBasic.IS` (use `TO_BE`) and `IS_NOT` (use `NOT_TO_BE`). Removed `assert`/`assertThat`. Added `createAndAppend` overload taking `Translatable`. Recommended splitting API files into `XyExpectations.kt`, `XyFeatureExtractor.kt` and `XySubjectChangers.kt`.\[6\] Switched to JDK 11 bytecode; JDK 11 code "cannot be inlined into jdk1.6 or jdk1.8". Empty creators now report `at least one expectation defined` (verified in tagged `creating/impl/CollectingExpectImpl.kt` and `creating/ErrorMessages.kt`). This project keeps JVM 21, its module split and public names at this migration step. |
| 1.0.0 | Moved to the current Kotlin Multiplatform plugin. "removed all deprecated function/classes/interfaces/translations etc. which were marked to be removed with 1.0.0 latest".\[3\] Also "we already removed en_GB from artifact names", e.g. `atrium-fluent-en_GB` became `atrium-fluent`.\[7\] A Spek→Kotest migration was tried and rolled back because Kotest broke kotlin-test on JS.\[2\]\[3\] |
| 1.1.0 | Introduced `expectGrouped`/`group`. Dropped Kotlin 1.2. Deprecated the `kotlin_1_3` extension package (functions moved into the main package).\[3\] Renamed `charsequence.contains` packages to `charsequence.toContain` (binary break).\[3\] |
| 1.2.0 | Added `toHoldThirdPartyExpectation` and `extractSubject`. Dropped Kotlin 1.3. JS now IR-only.\[8\] Deprecated all `atrium-translations-de_CH` enums.\[3\] |
| 1.3.0 | Removed `translations_de_CH`. `AtriumError` now extends opentest4j's `AssertionFailedError` on the JVM, which gives the IntelliJ diff link. Updated kbox to 3.x. "testFactory as preparation to get rid of spek".\[1\] api-infix: "This was the last release". The Proof rewrite was postponed to 2.x.\[1\]\[3\] |

**What is announced but not done (as of 1.3.0).** The v1.3.0 release notes say: "We will make a major refactoring on core and logic level (not before 2.1.0). Those changes should not affect most Atrium users." As part of it, Atrium deprecates "everything related to Translatable (Untranslatable, TranslatableWithArgs) => use Text" and "all Description...Expectation -> switch to Description...Proof". It also says "everything in atrium-logic, we move the functionality to core and will drop this module in the future (with 3.0.0 at the latest)". The breaking changes planned for 2.0.0 are:
- drop api-infix\[1\]
- "we will require jdk 17 and drop support for jdk 11"
- "we will drop the support for Kotlin 1.4 (most likely we will require Kotlin 2.0)"
- "most likely drop `en_GB` from package names"\[1\]\[9\]

3.0.0 "will remove everything deprecated in 2.x i.e. everything related to Assertions, Translations etc."\[9\] So `ProofContainer`/`_core`/`_coreAppend` are future-facing names. They briefly appeared in v1.3.0-alpha-1, described as "Proof and subtypes as successor of Assertion"; per the v1.3.0 release notes, "v1.3.0-alpha-1 and v1.3.0-alpha-2 contained work of it but was dropped from the v1.3.0 release in the end". Do not code against them yet.

**Translatable → what?** In 1.3.0, the README's own extension examples use plain `String` descriptions and `Text(...)` representations, e.g. `_logic.createAndAppend("is", Text("an even number")) { … }`.\[4\] `Translatable`, `Untranslatable` and `DescriptionBasic` still exist in `atrium-translations-en_GB`, but the v1.3.0 release notes state "we will drop the support for internationalization with 3.0.0, all functions involving Translatable will be deprecated in v2.0.0". Moving an extension's own `Translatable` enums (e.g. `translations/en` in atrium-gradle-testkit) to `String` constants is the cheapest forward-compatible step.

**Feature extraction API.** `feature` (reflection-based, using the property/method name as description), `its` (lambda, with `its.definedIn(File.kt:line)` as description) and `feature("description") { … }` all exist in 1.x.\[10\] The README advises that inside your own expectation functions you avoid `its` and the `MetaFeature` lambda (`feature { f(it::x) }`). Pass a class reference instead (`feature(Person::children)`), so "we can always show the feature name, also in case a previous feature extraction or subject transformation failed". For extractions that are not always safe (like `List.get`), use `_logic.extractFeature`.\[8\]\[10\] `extractSubject` (added in 1.2) is discouraged except for data that depends on random generation.\[4\]

**`expect(...) { }` / `expectGrouped`.** An `assertionCreator` lambda "has always the semantic of an expectation-group", meaning not fail-fast, and Atrium can report sub-expectations even when the subject is absent.\[4\] `expectGrouped { group("…") { expect(x)… } }` (1.1) groups unrelated subjects.\[11\] `ExpectationCreator<T>` is a typealias for `Expect<T>.() -> Unit`.\[4\]

### 2. API design conventions for expectation functions (fluent API)

- **Naming:** use the `to + infinitive` schema (0.17), e.g. `toEqual`, `toBeLessThan`, `toContain`, `toHaveSize`, `toHaveElementsAndAll`, `notToContain`, `notToEqualNull`.\[2\]\[12\] For TestKit this means `wasSuccessful()`/`wasInvoked()`-style names should become something like `toHaveOutcome`, `toBeSuccessful` and `toHaveTask`. You can keep the old names as `@Deprecated(ReplaceWith(...))` aliases, which is the same mechanism Atrium uses for its own migrations.\[8\]
- **Feature-extractor naming:** shortcut properties are named after the feature, without the `get` prefix: `message`, `first`, `second`, `keys`, `values`, `size`. Safe-extraction functions are named after the operation (`get(index)`, `getExisting(key)`).\[4\]\[10\]
- **Overload pairs:** feature extractors and narrowing functions always come in two overloads. One takes an `assertionCreator` lambda and returns the *original* `Expect<T>`. The other takes the same parameters minus the lambda and returns `Expect<R>`, changing the subject.\[4\]\[8\]\[13\] The README's composition example is the template:\[4\]
  ```kotlin
  val Expect<Person>.children: Expect<Collection<Person>> get() = feature(Person::children)
  fun Expect<Person>.children(assertionCreator: Expect<Collection<Person>>.() -> Unit): Expect<Person> =
      feature(Person::children, assertionCreator)
  ```
- **Generics:** "You should always do this unless your type is final … but to have a simple rule, just do it", i.e. `fun <T : BuildResult> Expect<T>.…`, because `Expect` is invariant.\[4\] This matters for Gradle's `BuildResult`/`BuildTask`, which are interfaces.
- **Packages:** Atrium's own functions live in `ch.tutteli.atrium.api.fluent.en_GB`,\[4\] but `en_GB` is likely to be dropped in 2.0.0.\[1\] An extension should use its own package (atrium-gradle-testkit uses `de.joshuagleitze.test.gradle`) and must not reuse Atrium's package names.\[14\]
- **Infix:** `apis/differences.md` documents how infix mirrors fluent (e.g. `expect(x) toEqual 2`, `toBe empty`).\[15\] Since 1.3.0 is the last infix release, new extensions should ship fluent only.\[1\]

### 3. Module structure in 1.x and what an extension should depend on

The Atrium repo's top level in 1.x is: `apis/` (`atrium-api-fluent`, `atrium-api-infix`), `atrium-core`, `logic/atrium-logic`, `translations/atrium-translations-en_GB`, `bundles/` (`atrium-fluent`, `atrium-infix`), `misc/` (verbs, specs, tools, readme-examples) and `samples/`. Bundles "bundle API, logic, core, translation as well as predefined expectation verbs".\[4\]\[11\] Files follow `apis/fluent/atrium-api-fluent/src/{commonMain,jvmMain}/kotlin/ch/tutteli/atrium/api/fluent/en_GB/xyExpectations.kt`, with logic implementations at `logic/atrium-logic/src/{commonMain,jvmMain}/kotlin/ch/tutteli/atrium/logic/impl/DefaultXyAssertions.kt`. All modules are Kotlin Multiplatform. With Kotlin ≥ 1.5 consumers use the unsuffixed artifact (e.g. `atrium-fluent`); `-jvm`/`-js` suffixes are only needed for older Kotlin.\[4\]\[11\] `main` already builds as `2.0.0-SNAPSHOT`.\[16\]

**Recommended dependencies for a library like atrium-gradle-testkit** (my recommendation; Atrium does not publish a guide for this):
- `api("ch.tutteli.atrium:atrium-api-fluent:1.3.0")`, because your public signatures expose `Expect<T>`.
- `api` or `implementation` on `atrium-logic`, since `_logic` is needed for custom descriptions and safe extraction. The README examples import `ch.tutteli.atrium.logic._logic`.\[4\]\[17\]
- Do **not** depend on the `atrium-fluent` bundle or `atrium-verbs` in `main`. Users bring their own verb, and the README even shows excluding `atrium-verbs` to define your own.\[4\]\[11\]
- No Atrium BOM was found in the sources reviewed. Pin a single `atriumVersion`.
- Drop `atrium-translations-*` once descriptions are `String`s.

**Minimum versions for 1.3.0:** "Kotlin: 1.4, JVM: 11, JS: IR-Backend".\[4\]\[11\] Atrium itself builds with a newer compiler (1.1.0 used "kotlin 1.8.21 but api/languageLevel 1.4") and officially supports JDK 17.\[3\]

### 4. How Atrium tests its own expectation functions

- **Historically (0.x–1.2):** Spek 2 specs. Abstract integration specs live in `atrium-specs` (e.g. `ch.tutteli.atrium.specs.integration.MapFeatureAssertionsSpec`). Each API module subclasses them, passing function references via helpers like `property<…>(Expect<Map<…>>::keys)`, `fun1`, `fun2` and `feature1`.\[18\] That is how one spec is parameterised over fluent and infix: the infix module passes companion-object wrappers.\[19\]
- **Now:** "migrate away from Spek to kotlin-test" (#1963, open). The v1.3.0 release notes list 19 spec migrations to kotlin-test under "Improvements", e.g. CharSequenceExpectationsSpec (#1954/PR #1993), CollectionExpectationsSpec (#2009/PR #2082) and IterableToContainInAnyOrderOnlyEntriesExpectationsSpec (#2024/PR #2188); others remain open on the tracker, such as #2029 IterableToContainInOrderOnlyValuesExpectationsSpec, opened Jun 25, 2025. 1.3.0 added "testFactory as preparation to get rid of spek" (PR #1822), and the release notes list "rewrite :readme-examples from spek to junit5" (issue #1681, PR #1834). In PR #2202 the maintainer said the plan is to drop infix so that "tests become simpler (no abstract class in atrium-specs needed any more)".\[20\]
- **Samples:** every API function's KDoc references a sample via `@sample`.\[21\]\[22\] Samples live in `src/commonTest`/`src/jvmTest` under `ch.tutteli.atrium.api.fluent.en_GB.samples` (e.g. `AnyExpectationSamples`, `FeatureExtractorSamples`), so they are compiled and run as tests.\[4\]\[23\] The convention from Atrium's contributor issues: one sample function per API function,\[21\] and the overload without `assertionCreator` gets the suffix `Feature`.\[24\]
- **Implication:** `atrium-specs` is an internal, Spek-based, moving target. The only known reference extension, atrium-gradle-testkit, already moved its 83 assertion tests from Spek to Kotest 5.9.1 and excluded Atrium's transitive Spek engine.\[25\] I could not verify the exact signature of a 1.3 `ExpectationFunctionBaseTest`/`testFactory` helper, so do not build on it.

### 5. Documentation conventions

- KDoc on every public function, using `@return` and `@since`. Atrium issues require "add @since X.Y.Z … to KDOC of val and fun".\[26\] Atrium's KDoc distinguishes "The newly created [Expect] for the extracted feature" (subject-changing overload) from "an [Expect] for the subject of `this` expectation" (lambda overload). Atrium also adds `@sample` pointing at a test-source samples class.\[22\]
- Dokka generates the HTML; docs.atriumlib.org hosts one version per release (latest is 1.3.0) with a version switcher,\[27\] and Atrium documents samples from `commonTest` and `jvmTest`.\[3\]\[4\]\[11\]\[28\] atrium-gradle-testkit migrated to Dokka 2.2.0 and publishes its docs on GitHub Pages.\[14\]\[29\]
- README structure: Atrium's README has Installation (with minimum requirements), Examples, "Write own Expectation Functions", "Use own Expectation Verb", API Styles and FAQ, and each section has an Example↔Output pair.\[4\] For an extension, a short README with an example, a "Compatibility" section (atrium-gradle-testkit states "Kotlin ≥ 2.2, Java ≥ 17") and a link to the Dokka output is the established pattern.\[14\]

### 6. Reference implementations

- **jGleitz/atrium-gradle-testkit** is the canonical third-party extension and is listed under "Third-party Extensions" in older Atrium READMEs.\[5\]\[30\] It is **still on Atrium 0.16.0**.\[25\] The modules are `apis/fluent-en`, `logic`, `translations/en` and `api-spec`, and the published `atrium-gradle-testkit-logic` 1.0.1 depends on `atrium-core-api` and `atrium-logic`.\[14\]\[31\] A Renovate PR (#171, **closed as "autoclosed" on Oct 26, 2026** – verified Oct 27, 2026) had proposed bumping `atrium-logic` and `atrium-specs` to 1.2.0 and `atrium-core-api` to 1.0.0. That bump will not compile as-is, because `atrium-core-api` was renamed to `atrium-core` in 0.17.\[2\] Its Sept 2026 modernisation steps (Kotest, Dokka 2, Gradle 9.7.1 – the build has since moved to Gradle 9.8.2 – `testkit` as an `api` dependency) are a reasonable model for build hygiene, but **it is not a 1.x reference implementation**.\[25\]\[29\]\[32\]
- **robstoll/niok** (by the Atrium author) uses `atrium-fluent` 1.3.0 for its tests (Renovate PR #272, merged Oct 22, 2026).\[9\] It is a consumer, not an extension.
- I found no published third-party extension already ported to 1.x (no "atrium-ktor" or similar). The best 1.x reference is Atrium's own `api-fluent` + `atrium-logic` sources, e.g. `DefaultListAssertions` for safe `get` extraction, `DefaultFloatingPointAssertions` for explanatory assertions and hints, and `DefaultAnyAssertions` for reported subject changes. The README links all of these as templates.\[4\]

## Details: Before (0.16) vs. After (1.3) Code

The 0.16 snippets show the typical shape of an extension written against 0.16 (`ExpectImpl`/`_logic` with `Translatable` enums). The "after" snippets follow patterns shown verbatim in the 1.3 README. Lines marked **(verify)** use logic-level builder names I could not check against 1.3 sources.

### (a) Simple custom expectation with a description

```kotlin
// BEFORE (0.16, with an own translation enum in atrium-gradle-testkit-translation-en)
enum class DescriptionBuildTask(override val value: String) : StringBasedTranslatable {
    OUTCOME("has outcome")
}
fun <T : BuildTask> Expect<T>.hasOutcome(expected: TaskOutcome): Expect<T> =
    _logic.createAndAppendAssertion(DescriptionBuildTask.OUTCOME, expected) { it.outcome == expected }
```

```kotlin
// AFTER (1.3) — String description + value/Text representation; no translation module
import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.logic._logic
import ch.tutteli.atrium.reporting.Text

/**
 * Expects that the subject of `this` expectation (a [BuildTask]) has the given [expected] outcome.
 *
 * @return an [Expect] for the subject of `this` expectation.
 * @since 2.0.0  // version of *your* library
 * @sample de.joshuagleitze.test.gradle.samples.BuildTaskExpectationSamples.toHaveOutcome
 */
fun <T : BuildTask> Expect<T>.toHaveOutcome(expected: TaskOutcome): Expect<T> =
    _logic.createAndAppend("to have outcome", expected) { it.outcome == expected }

fun <T : BuildTask> Expect<T>.toBeSuccessful(): Expect<T> =
    _logic.createAndAppend("to be", Text("successful")) { it.outcome == TaskOutcome.SUCCESS }

@Deprecated("Use toBeSuccessful", ReplaceWith("toBeSuccessful()"))
fun <T : BuildTask> Expect<T>.wasSuccessful(): Expect<T> = toBeSuccessful()
```

This is the README's own pattern: `_logic.createAndAppend(description: String, representation, test)` "creates and appends the expectation" and returns `Expect<T>`.\[8\] `Text` is used "so that `"an even number"` is not treated as a `String` in reporting".\[4\] If you prefer to stay entirely on the API level, you can also compose existing functions: `feature(BuildTask::getOutcome) { toEqual(TaskOutcome.SUCCESS) }`.

### (b) Feature extraction with both overloads

```kotlin
// BEFORE (0.16 style)
val <T : BuildResult> Expect<T>.output: Expect<String>
    get() = feature(BuildResult::getOutput)
fun <T : BuildResult> Expect<T>.output(assertionCreator: Expect<String>.() -> Unit): Expect<T> =
    feature(BuildResult::getOutput, assertionCreator)
```

```kotlin
// AFTER (1.3) — essentially unchanged API-wise; only naming/KDoc/description adapt
/**
 * Creates an [Expect] for the output of the subject of `this` expectation,
 * so that further fluent calls are made on it.
 *
 * @return The newly created [Expect] for the extracted feature.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildResultFeatureExtractorSamples.outputFeature
 */
val <T : BuildResult> Expect<T>.output: Expect<String>
    get() = feature("output", BuildResult::getOutput)

/**
 * Expects that the output of the subject of `this` expectation
 * holds all expectations the given [assertionCreator] creates for it.
 *
 * @return an [Expect] for the subject of `this` expectation.
 * @since 2.0.0
 * @sample de.joshuagleitze.test.gradle.samples.BuildResultFeatureExtractorSamples.output
 */
fun <T : BuildResult> Expect<T>.output(assertionCreator: Expect<String>.() -> Unit): Expect<T> =
    feature("output", BuildResult::getOutput, assertionCreator)
```

The extractor is a Java getter, so `feature(BuildResult::getOutput)` would report `getOutput`. The README notes that a Kotlin property reference does not exist for Java getters,\[4\] so using the `String`-description overload (`feature("…", extractor)` / `feature("…", extractor, assertionCreator)`) gives the readable name `output`. **(verify)** that the two-argument class-reference form compiles without ambiguity under your Kotlin version, since the README warns about Kotlin overload bugs with `feature`.\[4\]

### (c) Safe feature extraction (task may not exist), the "logic" layer

```kotlin
// BEFORE (0.16): ExpectImpl.feature.extractor(this)…  /  _logic.extractFeature… with Translatable + RawString
```

```kotlin
// AFTER (1.3) — logic-level helper kept internal, API overloads delegate to it  (verify builder names)
import ch.tutteli.atrium.core.None
import ch.tutteli.atrium.core.Some
import ch.tutteli.atrium.creating.AssertionContainer
import ch.tutteli.atrium.logic._logic
import ch.tutteli.atrium.logic.extractFeature
import ch.tutteli.atrium.reporting.Text

internal fun <T : BuildResult> AssertionContainer<T>.task(path: String) =
    extractFeature
        .withDescription("task $path")
        .withRepresentationForFailure(Text("❗❗ task was not part of the build"))
        .withFeatureExtraction { result -> result.task(path)?.let { Some(it) } ?: None }
        .withoutOptions()
        .build()

/** @return The newly created [Expect] for the extracted feature. @since 2.0.0 */
fun <T : BuildResult> Expect<T>.task(path: String): Expect<BuildTask> =
    _logic.task(path).transform()

/** @return an [Expect] for the subject of `this` expectation. @since 2.0.0 */
fun <T : BuildResult> Expect<T>.task(path: String, assertionCreator: Expect<BuildTask>.() -> Unit): Expect<T> =
    _logic.task(path).collectAndAppend(assertionCreator)
```

This mirrors what the README describes: "in case it is not always safe to call a method (e.g. `List.get`) … wrap it into an own feature extractor and use `_logic.extractFeature`",\[10\] with `DefaultListAssertions` as the template.\[4\]\[8\] The exact `Some`/`None` package and the `transform()`/`collectAndAppend()` step names are **(verify)** items; confirm them against `DefaultListAssertions.kt` in `logic/atrium-logic` at tag `v1.3.0`.

**How the logic layer should be organised now.** In 0.16 an extension typically had a separate `logic` module with `AssertionContainer<T>.xy()` functions and an API module calling `_logicAppend { xy() }`.\[17\] That still works in 1.3, but Atrium says atrium-logic will be folded into core and removed "with 3.0.0 at the latest", and that `AssertionContainer`/`Assertion` will be renamed under the Proof rewrite in 2.x.\[1\] My recommendation:
- Collapse the extension's separate `logic` and `translations` modules into the API module.
- Keep logic-level helpers `internal`, as above, so the 2.x migration is a private refactor rather than a breaking change for your users.

### (d) Tests (replacing `SubjectLessSpec` / atrium-specs)

```kotlin
// AFTER — plain kotlin-test / JUnit 5, no atrium-specs
class BuildTaskExpectationsTest {
    @Test fun toHaveOutcome_holds() { expect(fakeTask(SUCCESS)).toHaveOutcome(SUCCESS) }

    @Test fun toHaveOutcome_fails_and_reports() {
        expect { expect(fakeTask(FAILED)).toHaveOutcome(SUCCESS) }
            .toThrow<AssertionError> { messageToContain("to have outcome", "SUCCESS") }
    }

    @Test fun lambdaOverload_reportsSubExpectations_whenTaskMissing() {
        expect { expect(fakeResult()).task(":missing") { toBeSuccessful() } }
            .toThrow<AssertionError> { messageToContain("task :missing", "to be", "successful") }
    }
}
```

The third test replaces what `SubjectLessSpec` checked in 0.16. It verifies that the lambda overload still reports its sub-expectations when the subject is absent, which Atrium's README names as the main benefit of `assertionCreator` overloads.\[4\] Put `@sample` functions in a `…samples` package in test sources so they compile and run with the build, as Atrium does.

## Recommendations (migration plan)

1. **Bump the toolchain first.** Kotlin ≥ 1.4 (in practice the same 2.x compiler you already use), JVM bytecode target ≥ 11. This project builds with launcher JDK 27 and a Java toolchain of 21 or newer (CI tests 21, 25 and 27). The Java 8 bytecode target is already gone: the current main targets Java 21 bytecode (a deliberate decision, commit `f9c25e0`), which also satisfies the constraint that Atrium inline functions compiled for JDK 11 cannot be inlined into JDK 8 bytecode.\[3\]\[25\]\[29\]
2. **Fix the dependencies.** Replace `atrium-core-api` with `atrium-core` (or rely on it transitively), `atrium-api-fluent-en_GB` with `atrium-api-fluent`, keep `atrium-logic`, remove `atrium-specs` and all `atrium-translations-*`, and exclude any transitive Spek engine.
3. **Migrate API names to `to + infinitive`**, keeping the old names as `@Deprecated(ReplaceWith)` shims for one release. *(Superseded: the owner-approved major-release plan ships no deprecated aliases; removed names are documented in the changelog instead.)*
4. **Replace translation enums with `String` descriptions and `Text` representations.** Remove `RawString`, `Untranslatable` and `DescriptionBasic` usages. Fold the `translations` and `logic` modules into the API module, with internal helpers.
5. **Use class-reference `feature(...)` or `String`-described `feature(...)`** for property-like features, and `_logic.extractFeature` for `task(path)`. Always provide both overloads.
6. **Rewrite tests** as kotlin-test/JUnit 5 (or keep Kotest), adding `@sample`-referenced sample classes and `@since` tags.
7. **Ship fluent only.** Do not invest in an infix variant (it ends with 1.3.0), and use your own package without `en_GB`.
8. **Plan a 2.x follow-up:** expect to move `_logic`/`AssertionContainer` usage to the Proof-based core API once Atrium 2.x ships its migration guide. Keep that surface small.

## Caveats

- **Unverified source details:** I could not retrieve verbatim 1.3 source files (`pairFeatureExtractors.kt`, `anyExpectations.kt`, `DefaultListAssertions.kt`, the kotlin-test spec base classes). The exact logic-builder step names in example (c), the `Some`/`None` package and any `ExpectationFunctionBaseTest`/`testFactory` signature should be checked against tag `v1.3.0` before coding. Examples (a), (b) and (d) use patterns shown verbatim in the 1.3 README.
- **Version-number inconsistency in Atrium's own notes:** the timeline for dropping i18n and Assertion slipped repeatedly (announced for 0.19/0.20, then 1.2, 1.3, 1.4/1.5, now 2.x/3.0).\[1\]\[2\]\[3\] Treat the 2.x plans as intentions, not commitments.
- **README vs. tag:** the GitHub `main` README is a "sneak peek at the next version"\[4\] (`main` is 2.0.0-SNAPSHOT).\[16\]\[33\] Read the README at tag `v1.3.0` for exact 1.3 behaviour.
- **No BOM and no official extension-author guide** were found. The dependency advice (api vs. implementation) is my recommendation based on how the API types leak into public signatures.

## Quellen

1. [Release v1.3.0 OffsetDateTime, (not)toBeIn, opentest4J · robstoll/atrium · GitHub](https://github.com/robstoll/atrium/releases/tag/v1.3.0)
2. [fix(deps): update atriumversion to v1 by renovate\[bot\] · Pull Request #171 · jGleitz/atrium-gradle-testkit](https://github.com/jGleitz/atrium-gradle-testkit/pull/171)
3. [Releases · robstoll/atrium](https://github.com/robstoll/atrium/releases)
4. [GitHub - robstoll/atrium: A multiplatform expectation library for Kotlin](https://github.com/robstoll/atrium)
5. [A multiplatform assertion library for Kotlin](https://androidrepo.com/repo/robstoll-atrium-android-test)
6. [Official Atrium v0.18.0 release notes](https://github.com/robstoll/atrium/releases/tag/v0.18.0); [tagged sources](https://github.com/robstoll/atrium/tree/v0.18.0)
7. [Maven Repository: ch.tutteli.atrium » atrium-fluent-en\_GB](https://mvnrepository.com/artifact/ch.tutteli.atrium/atrium-fluent-en_GB)
8. [GitHub - jGleitz/atrium: A multiplatform assertion library for Kotlin · GitHub](https://github.com/jGleitz/atrium)
9. [Update dependency ch.tutteli.atrium:atrium-fluent to v1.3.0 by renovate\[bot\] · Pull Request #272 · robstoll/niok · GitHub](https://github.com/robstoll/niok/pull/272)
10. [GitHub - robstoll/atrium: A multiplatform expectation library for Kotlin · GitHub](https://libraries.io/github/robstoll/atrium)
11. [atrium/README.md at main · robstoll/atrium · GitHub](https://github.com/robstoll/atrium/blob/main/README.md)
12. [github.com](https://github.com/robstoll/atrium-roadmap/issues/93)
13. [ch.tutteli.atrium:atrium-api-cc-en\_UK - Maven Central Packag...](https://socket.dev/maven/package/ch.tutteli.atrium/atrium-api-cc-en_UK)
14. [GitHub - jGleitz/atrium-gradle-testkit: Atrium assertions to test Gradle plugins with TestKit](https://github.com/jGleitz/atrium-gradle-testkit)
15. [atrium/apis/differences.md at main · robstoll/atrium · GitHub](https://github.com/robstoll/atrium/blob/main/apis/differences.md)
16. [atrium/build.gradle.kts at main · robstoll/atrium · GitHub](https://github.com/robstoll/atrium/blob/main/build.gradle.kts)
17. [atrium Alternatives - Kotlin Tests](https://kotlin.libhunt.com/atrium-alternatives)
18. [add Map features to Expect · robstoll/atrium@066d35d · GitHub](https://github.com/robstoll/atrium/commit/066d35dd4dda360cbb12bd4123ce53795af551f6)
19. [mapAssertions for the new infix API · Issue #229 · robstoll/atrium · GitHub](https://github.com/robstoll/atrium/issues/229)
20. [add expectation functions for OffsetDateTime by robstoll · Pull Request #2202 · robstoll/atrium · GitHub](https://github.com/robstoll/atrium/pull/2202)
21. [add samples for chronoZonedDateTimeExpectations of api-fluent · Issue #916 · robstoll/atrium · GitHub](https://github.com/robstoll/atrium/issues/916)
22. [Use @sample block tags in KDoc to embed samples inside the API · Issue #624 · robstoll/atrium · GitHub](https://github.com/robstoll/atrium/issues/624)
23. [add samples for featureAssertions of api-fluent · Issue #648 · robstoll/atrium · GitHub](https://github.com/robstoll/atrium/issues/648)
24. [add samples for optionalExpectations of api-fluent · Issue #917 · robstoll/atrium · GitHub](https://github.com/robstoll/atrium/issues/917)
25. [test: migrate from Spek to Kotest by jGleitz · Pull Request #166 · jGleitz/atrium-gradle-testkit · GitHub](https://github.com/jGleitz/atrium-gradle-testkit/pull/166)
26. [Result.isSuccess · Issue #203 · robstoll/atrium · GitHub](https://github.com/robstoll/atrium/issues/203)
27. [Atrium Code Documentation Overview](https://docs.atriumlib.org/)
28. [Sample reference results in "Unresolved function" for one particular file but not for all methods · Issue #3016 · Kotlin/dokka · GitHub](https://github.com/Kotlin/dokka/issues/3016)
29. [build: migrate to Dokka 2 by jGleitz · Pull Request #167 · jGleitz/atrium-gradle-testkit · GitHub](https://github.com/jGleitz/atrium-gradle-testkit/pull/167)
30. [github.com](https://github.com/ReccaFlames/atrium)
31. [de.joshuagleitze:atrium-gradle-testkit-logic - Maven Central](https://central.sonatype.com/artifact/de.joshuagleitze/atrium-gradle-testkit-logic)
32. [build: update to Gradle 9.7.1 and Java 26 by jGleitz · Pull Request #168 · jGleitz/atrium-gradle-testkit · GitHub](https://github.com/jGleitz/atrium-gradle-testkit/pull/168)
33. [github.com](https://github.com/szatyinadam/atrium)
