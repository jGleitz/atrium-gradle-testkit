# atrium-gradle-testkit Modernization Plan

> **Coordinator**: Sisyphus (AI)
> **Last Updated**: 2026-09-17
> **Status**: PR #168 contains the nine committed Gradle 9.7.1 modernization commits at `e06d42c`; its original Java 17/25/26 matrix passed remotely. The uncommitted follow-up in this worktree introduces the Java 17 minimum/toolchain design and still requires independent review and a full remote matrix rerun. No publication, credential, signing, or Central Portal validation was performed.

This document persists the modernization work for this project. Any agent can pick up where the previous one left off by reading this file and the git history.

---

## Goal

Bring the `atrium-gradle-testkit` project up-to-date: modern Java, modern Gradle, modern Kotlin, replace Spek with kotest, and prepare for Atrium upgrade.

## Task List (from the original brief)

- [x] **Task 0**: Disable the GitHub build steps that would release the library
- [x] **Task 1**: Get the CI green again (Phase 0-1 merged in PR #162)
- [ ] **Task 2**: Get it to build with Java 26 (PR #168 baseline CI passed; the uncommitted Java 17 toolchain follow-up still needs independent review and remote CI)
- [ ] **Task 3**: Update all libraries and tools **except Atrium** (Atrium update requires larger changes, deferred)
- [x] **Task 4**: Migrate from Spek to Kotest (PR #166 merged as `4bee3c1` after Java 8/11/16 CI)
- [ ] **Task 5**: Prepare an overview of architecture changes needed for latest Atrium (do NOT implement, just prepare information for next agent)

## Current State (validated local Gradle 9 and Java 26 modernization)

| Component | Current Version | Notes |
|-----------|----------------|-------|
| Gradle | 9.7.1 | locally validated wrapper on the final branch |
| Kotlin | 2.4.20 | root and generated fixture aligned |
| Java | 17 minimum, 26 launcher | **`.sdkmanrc` selects Temurin 26, Gradle defaults to a Java 17 toolchain, and published classes target Java 17 bytecode, major version 61.** |
| Kotest | 5.9.1 | current test framework; all migrated suites use `FunSpec` |
| Atrium | 0.16.0 | **frozen** until other updates are done |
| Spek / spek-testfiles | removed | executable Spek dependencies removed; scoped exclusions block transitive `org.spekframework.spek2` and `ch.tutteli.spek` artifacts |
| Dokka | 2.2.0 | DGP v2 prerequisite merged in PR #167 as `bf26fd7` |
| palantir git-version | 3.4.0 | versioning |
| nexus-publish | 2.0.0 | local Gradle 9 update; publication architecture retained |
| CI | GitHub Actions | Temurin 26 launcher plus Temurin toolchains 17/25/26; the original PR #168 matrix passed, while this uncommitted split-launcher design still needs remote validation |

## Project Structure

```
├── api-spec/          # reusable Kotest FunSpec suites and local SubjectLessSpec registration support
├── apis/fluent-en/    # published fluent Atrium API with concrete Kotest suites
├── example-project/   # Kotest TestKit usage example with an isolated temporary project
├── logic/             # core assertion logic (published)
├── translations/en/   # English translations (published)
└── build.gradle.kts   # root build (handles publishing, dokka, versioning)
```

Published modules: `apis:fluent-en`, `logic`, `translations:en` (via `willBePublished` extra flag).
Root project: releases to GitHub Packages + Sonatype/Maven Central via `nexus-publish`, signed via the Gradle signing plugin.

## Phase Status

### Phase 0 — Safety: Disable Release Steps
- [x] Remove/comment out `release.yml` workflow
- [x] Remove/comment out `release` job in `ci.yml`
- [x] Disable automatic CI publication; local publication tasks remain available
- **Status**: Merged to master in PR #162 (`c95aaa2823209164572412da8e4d86251e739205`)

### Phase 1 — CI Green
- [x] Check what passes today (Build passed locally on Java 17)
- [x] Fix CI workflow (`adopt` → `temurin` to silence deprecation)
- [x] Phase 0-1 merged through PR #162
- **Status**: Merged. PR #168 subsequently moved the matrix to Java 17/25/26 and passed remotely; the uncommitted launcher/toolchain separation still needs a fresh matrix run.
- **Historical note**: At Phase 1 completion, `fluent-en` Spek tests silently ran **0 tests**. The merged Phase 4 migration now executes all 83 fluent tests.

### Phase 2 - Restarted Sequence: Gradle 8 Before Gradle 9
- [x] Preserve the mixed `modernize/gradle-9` WIP without editing, cleaning, resetting, or copying its changes
- [x] Create `/tmp/opencode/atrium-gradle-8` on `modernize/gradle-8` from merged `master`
- [x] Select latest stable Gradle 8.x from official release metadata and read upgrade notes
- [x] Upgrade Gradle 7.6.6 to 8.14.5; adapt the removed report DSL and generated fixture application DSL
- [x] Execute all four required Java 17 checks and inspect actual test results
- [x] Resolve the fixture blocker using the authorized alignment to existing Kotlin 1.9.25; replace `mainClassName` only after observing its failure
- [x] Obtain green `clean test`, `clean build --warning-mode all`, `checkDependenciesBeforePublishing`, and `release --dry-run` before any commit
- [x] After the merged Gradle 8 gate, begin individual library modernization with the Kotlin-only PR #161 step; other libraries remain pending
- [x] Upgrade Gradle 8.14.5 to 9.7.1, update Nexus Publish Plugin to 2.0.0, and validate Java 26 locally
- [x] Change CI from Java 8/11/16 to toolchains 17/25/26 because Gradle 9 requires Java 17+ to launch
- [x] Push the Gradle 9 baseline as PR #168 and pass its original remote Java 17/25/26 matrix
- [ ] Independently review and rerun the full matrix for the uncommitted Temurin 26 launcher/Java 17 toolchain follow-up
- **Status**: PR #168 exists and its committed baseline matrix passed. The Java 17 minimum/toolchain follow-up remains uncommitted and unpushed in the recovery worktree. The earlier mixed `modernize/gradle-9` experiment remains superseded and is not the final branch.

### Phase 3 — Library Updates (one at a time, one PR each)
- [x] Kotlin 1.9.25 to 2.4.20: present in base `9c505ac`; historical Java 17 validation is preserved below
- [x] Dokka 1.9.20 to 2.2.0 with DGP v2: PR #167 merged as `bf26fd7` after Java 8/11/16 CI
- [x] spek-testfiles replacement: local `SubjectLessSpec` registration support and native temporary directories, completed with the Kotest migration
- [x] Nexus Publish Plugin 0.4.0 to 2.0.0: locally complete with the Gradle 9 work
- [ ] Remaining tools and libraries, including any later git-version update, one at a time; preserve publication architecture unless an explicit change is approved
- **Status**: Kotlin, Kotest, Dokka, and the local Nexus update are complete. Task 3 remains open because not every tool and library has been completed.

### Phase 4: Spek to Kotest Migration
- [x] Replace all abstract and concrete Spek suites with Kotest `FunSpec`
- [x] Update `api-spec` reusable suites and port Atrium 0.16.0 `SubjectLessSpec` registration semantics locally
- [x] Update `apis/fluent-en` tests so the full suite is discovered and executed
- [x] Update `example-project` to Kotest
- [x] Replace spek-testfiles temporary-directory support with unique native temporary directories; clean successful specs, retain failed fixtures, and surface cleanup failures
- [x] Remove direct Spek dependencies and exclude the two Spek groups inherited through Atrium specs
- [x] Retain every test without suppression
- **Status**: Merged in PR #166 as `4bee3c1` after Java 8/11/16 CI passed.

### Phase 5 — Atrium Architecture Overview (documentation only)
- Analyze how latest Atrium (0.x → 1.x) changes affect the project
- Document required architectural changes
- Identify simplifications (fewer Gradle projects?)
- **Status**: Not started

## Working Agreements (from the brief)

1. **One library at a time, one branch, one PR**
2. **No major-version skips** — go through each major version sequentially
3. **Read release notes for each update**
4. **All test cases must stay (rewrappable, not removable)**
5. **If changing functionality, test it**
6. **If untested functionality is found, report it**
7. **Coordinator pattern**: Sisyphus orchestrates, subagents execute
8. **File persisted state**: update this file after each phase/subagent round
9. **Green before commit**: validation must complete before commit; merge only after CI succeeds; never publish during modernization validation

## Guardrails

- Every commit/PR must identify as AI-authored (git committer name change)
- No architecture/design changes without explicit approval — only adaptations
- Improvement proposals for architecture → propose to user first
- If unclear → ask before burning tokens

## Current Branch

`modernize/gradle-9.7.1-java-26` in
`/home/josh/Projekte/atrium-gradle-testkit-gradle9-recovery`, based on `bf26fd7`.
That base includes Kotest PR #166, merged as `4bee3c1` after the historical Java
8/11/16 CI matrix, and Dokka 2.2.0 DGP v2 prerequisite PR #167, merged as
`bf26fd7` after the same historical matrix. PR #168 contains nine committed
Gradle 9 modernization commits through `e06d42c`; its original Java 17/25/26
matrix passed remotely. The Java 17 minimum/toolchain changes described here
remain uncommitted and unpushed for independent review.

The original `/home/josh/Projekte/atrium-gradle-testkit` worktree was not
touched during this recovery.
The previously preserved `modernize/gradle-9` branch remains at `fe99052`, but
its former `/tmp/opencode/atrium-gradle-9-clean` worktree no longer exists. It
was an experimental, superseded attempt and must not be described as the final
branch.

Next move: independently review the uncommitted diff, then require the updated
Java 17/25/26 toolchain matrix to pass before merge.

## Gradle 9.7.1, Java 17 Toolchains, and Temurin 26 Launcher Evidence (2026-09-17)

The committed PR #168 baseline upgrades the wrapper from Gradle 8.14.5 to 9.7.1
and the Nexus Publish Plugin from 0.4.0 to 2.0.0. This follow-up adds
`.sdkmanrc` with `java=26-tem`, so contributors and CI use the latest Temurin 26
patch as the Gradle launcher. The lazily parsed `javaVersion` project property
defaults to 17 and rejects values below 17. Java-enabled subprojects use that
toolchain for compiler, test, and Javadoc tools, while Java `--release` and
Kotlin `jvmTarget` remain fixed at 17. Published classes therefore use Java 17
bytecode, major version 61, even in the Java 25 and 26 matrix jobs.

This raises the artifact and runtime minimum from Java 8 to Java 17. It is a
breaking compatibility change and must be identified as such in the release
commit and PR rather than described as launcher-only modernization.

The existing TestKit example receives the selected `javaVersion`, configures its
generated build with the same toolchain and JVM 17 target, executes
`:compileKotlin`, `:classes`, and `:run`, and continues to print `Hello World!`.
No test or assertion was removed or weakened. CI keeps check names
`Build (17)`, `Build (25)`, and `Build (26)`: the first setup-java invocation
uses `.sdkmanrc`, and a second non-default Temurin installation supplies the
matrix toolchain path to every Gradle invocation.

Local verification kept the Gradle launcher on Temurin 26. With no
`javaVersion` property, the example test executor launched on Temurin 17 with
`-DjavaVersion=17`; the explicit override launched it on Temurin 26 with
`-DjavaVersion=26`. Both real nested builds printed `Hello World!` and passed.
The default `clean build --warning-mode all` passed with XML totals of 14
`BuildResult` tests, 69 `BuildTask` tests, and 1 example test, all with zero
failures, errors, or skips. `javap -verbose` reported major version 61 for all
six published main class files.

Release and publication graph validation must use Java 17 explicitly:

```sh
./gradlew -PjavaVersion=17 checkDependenciesBeforePublishing --console=plain
./gradlew -PjavaVersion=17 release --dry-run --console=plain
```

Both commands passed locally; all three dependency checks executed and every
release action was skipped by the dry-run. The release dry-run verifies task
graph construction only. No live publication,
credential, signing, or Central Portal validation occurred, so live Central
compatibility remains unverified. The official Gradle 9.7.1 wrapper JAR SHA-256 is
`7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d`.

Known non-blocking output remains: Gradle 10 delegated-property and archive
deprecations, Dokka's Gradle `package-list` warning, and Java 25/26 native-access
and ClassGraph warnings. None was hidden or treated as proof of remote CI or live
publication compatibility.

## Gradle 8 Bridge Evidence (2026-09-12)

### Version Selection and Official Notes

- [Gradle 8 release metadata](https://services.gradle.org/versions/8) reports **8.14.5** as the latest released, final, non-snapshot 8.x version, built/released on 2026-05-07.
- [8.14.5 release notes](https://docs.gradle.org/8.14.5/release-notes.html) recommend 8.14.5 rather than 8.14. It fixes transformed classloader thread safety and included-build dependency exclusions, and includes the earlier 8.14.4 repository-disabling security fixes.
- [7.x to 8.0 upgrade guide](https://docs.gradle.org/8.14.5/userguide/upgrading_version_7.html#report_and_testreport_api_cleanup): `Report.enabled` is removed; replace `reports.junitXml.isEnabled = true` with `reports.junitXml.required.set(true)`. This remains the only root build-script change.
- The same guide removes legacy `ArtifactTransform`, `IncrementalTaskInputs`, and `JavaApplication.mainClassName` APIs, and sets a Kotlin Gradle Plugin minimum of 1.6.10. The root and now-aligned fixture Kotlin 1.9.25 satisfy that minimum. The fixture uses `mainClass.set("de.joshuagleitze.HelloWorldKt")` after a failing run confirmed the removed property.
- [Official Kotlin 1.9.25 release](https://github.com/JetBrains/kotlin/releases/tag/v1.9.25): reference for the existing root version reused by the fixture; no root Kotlin upgrade was performed.
- [8.x upgrade guide](https://docs.gradle.org/8.14.5/userguide/upgrading_version_8.html) covers the executable wrapper JAR and warnings for convention access, JCenter, automatic test framework dependencies, and test sources with no executed tests. Deprecations that still work on Gradle 8 were not modernized in this step.
- [Compatibility matrix](https://docs.gradle.org/8.14.5/userguide/compatibility.html): Java 17 is supported. `./gradlew --version` reports Gradle 8.14.5 and launcher Java 17.0.20; the reported embedded Kotlin 2.0.21 belongs to Gradle, not an update to this project's Kotlin 1.9.25 plugin.

### Historical Commands and Results

Every Gradle invocation used this environment, in the new worktree:

```sh
export JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/sdkman-cli/libexec/candidates/java/17.0.20-tem
export PATH="$JAVA_HOME/bin:$PATH"
```

The actual invocations supplied these assignments directly before `./gradlew`.
`--console=plain` only controls output formatting. No test exclusions, ignored
failures, plugin substitutions, or compatibility fallbacks were supplied.
Required Gradle 8 check logs are in `/tmp/opencode/gradle8-evidence/`, outside
both worktrees; pipelines used `set -o pipefail` to retain failure status.
The following table preserves earlier evidence; its pre-fix failures are
superseded by the final validation table below.

| Command | Result | Evidence |
|---------|--------|----------|
| `git worktree add -b modernize/gradle-8 /tmp/opencode/atrium-gradle-8 master` (with `GIT_MASTER=1`) | Exit 0 | New worktree HEAD `c95aaa2`; `/tmp/opencode` verified before creation |
| `./gradlew clean test --console=plain` before upgrade, Gradle 7.6.6 | Exit 0, `BUILD SUCCESSFUL in 49s`; 24 actionable tasks, 18 executed, 6 up-to-date | Example XML: 1 test, 0 failures/errors/skips; fluent-en HTML: 0 tests |
| `./gradlew wrapper --gradle-version=8.14.5 --console=plain` | Exit 0, 1s, 1 task executed under 7.6.6 | Distribution URL updated |
| `./gradlew wrapper --console=plain` before DSL adaptation | Exit 1, 49s | `build.gradle.kts:48`: unresolved reference `isEnabled` |
| `./gradlew wrapper --console=plain` after DSL adaptation | Exit 0, 5s, 1 task executed under 8.14.5 | Wrapper JAR and both launch scripts regenerated |
| `./gradlew --version` | Exit 0 | Gradle 8.14.5, launcher JVM 17.0.20, daemon Java home resolves to the requested Temurin installation |
| `./gradlew clean test --console=plain` on Gradle 8.14.5 | Exit 1, `BUILD FAILED in 55s`; 24 tasks executed | `clean-test.log`; example XML: 1 test, 1 failure; fluent-en: 0 tests |
| `./gradlew clean build --warning-mode all --console=plain` | Exit 1, `BUILD FAILED in 51s`; 35 tasks executed | `clean-build.log`; same example test failure; all three published modules' main, source and Dokka JARs generated |
| `./gradlew checkDependenciesBeforePublishing --console=plain` | Exit 0, `BUILD SUCCESSFUL in 7s`; 3 tasks executed | `check-dependencies.log`; dependency checks executed for logic, fluent-en and translations |
| `./gradlew release --dry-run --console=plain` | Exit 0, `BUILD SUCCESSFUL in 1s` | `release-dry-run.log`; all task actions skipped, no publication attempted |
| `./gradlew -p '/tmp/opencode/atrium-gradle-8/example-project/build/test-outputs/[KotlinPluginSpek]/testProject' run --stacktrace --console=plain` | Exit 1, `BUILD FAILED in 26s` | `fixture-stacktrace.log`; directly reproduces removed API call inside Kotlin scripting plugin |
| `./gradlew --help` | Exit 0 | CLI usage displayed through regenerated wrapper |
| `./gradlew --not-a-gradle-option` | Exit 1, expected negative CLI check | Reports unknown command-line option and displays usage |
| `sh -n gradlew` | Exit 0 | POSIX shell syntax valid |
| `sha256sum gradle/wrapper/gradle-wrapper.jar` | Exit 0 | `7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`, matches official release metadata |

### Final Java 17 Validation

All commands below ran in `/tmp/opencode/atrium-gradle-8` with the explicit
`JAVA_HOME` and `PATH` assignments above. The four gates were run sequentially
so their clean/build operations could not interfere with one another.

| Command | Exit Code | Observed Result / Evidence Log |
|---------|-----------|--------------------------------|
| `./gradlew :example-project:test --rerun-tasks --console=plain` before the application DSL fix | 1 | 38s, 12 tasks executed; 1 test, 1 failure; XML identified `Unresolved reference: mainClassName`; `example-before-mainclass.log` |
| `./gradlew :example-project:test --rerun-tasks --console=plain` after the application DSL fix | 0 | 19s, 12 tasks executed; `example-after-mainclass.log` |
| `./gradlew clean test --console=plain` | 0 | 48s, 24 tasks executed; `final-clean-test.log` |
| `./gradlew clean build --warning-mode all --console=plain` | 0 | 57s, 35 tasks executed; `final-clean-build.log` |
| `./gradlew checkDependenciesBeforePublishing --console=plain` | 0 | 628ms, all 3 published-module checks executed; `final-check-dependencies.log` |
| `./gradlew release --dry-run --console=plain` | 0 | 591ms, every action skipped; `final-release-dry-run.log` |
| `./gradlew --version` | 0 | Gradle 8.14.5; launcher JVM 17.0.20; daemon home resolves to the requested Temurin installation |

The final release dry-run explicitly includes these three project paths:

- `:apis:atrium-gradle-testkit-fluent-en`
- `:atrium-gradle-testkit-logic`
- `:translations:atrium-gradle-testkit-translation-en`

For each, the graph retains `checkDependenciesBeforePublishing`, `sourcesJar`,
`dokkaJavadoc`, `dokkaJar`, Maven metadata/POM generation,
`initializeSonatypeStagingRepository`, `signMavenPublication`,
`publishMavenPublicationToSonatypeRepository`, `publishToSonatype`,
`publishMavenPublicationToGitHubPackagesRepository`,
`publishAllPublicationsToGitHubPackagesRepository`, and `release`.
Root `closeRepository`, `releaseRepository`, and `closeAndReleaseRepository`
remain in the graph. Dry-run proves graph construction only, not credentials,
signing execution, or live repository compatibility. No publication ran.

### Test Evidence and Resolved Fixture Failures

The baseline example result was read before upgrading/cleaning:
`example-project/build/test-results/test/TEST-KotlinPluginSpek.xml` had
`tests="1" skipped="0" failures="0" errors="0"`, timestamp
`2026-09-12T15:27:37`, and showed `Hello World!` from the nested build.
Its old Kotlin daemon logged an access error but completed using its existing
fallback; no fallback was added by this work.

The earlier Kotlin 1.4.10 fixture failed during plugin application on removed
`DependencyHandler.registerTransform(Action)`. The authorized fixture-only
alignment to existing Kotlin 1.9.25 was already present when final validation
began. The required first rerun then failed on `mainClassName`, confirmed in
the XML at `2026-09-12T16:15:01.557Z` (1 test, 1 failure, 0 errors/skips).
Only then was that generated DSL changed to `mainClass.set(...)`; the existing
test and its assertions were neither removed nor weakened.

After the final clean build, the XML was read directly and reports exactly
`tests="1" skipped="0" failures="0" errors="0"`, timestamp
`2026-09-12T16:18:01.061Z`. The case remains
`KotlinPluginSpek / run / compiles the Kotlin code and runs it`.
Its real TestKit nested build executes `:compileKotlin`, `:classes`, and `:run`,
prints `Hello World!`, and exits successfully in 43s. This exercises the actual
generated application and the existing Atrium assertions, not merely compilation.

An additional standalone `run --rerun-tasks` attempt against that generated
fixture path after the successful tests exited 1 because the directory was no
longer present (`final-fixture-run.log`). It is not a gate failure or evidence
against the successful nested run recorded in the XML; no fixture was recreated
or test lifecycle changed for this optional check.

The fluent API report, `apis/fluent-en/build/reports/tests/test/index.html`,
was read after the final clean build and shows **0 tests, 0 failures, 0 ignored**,
generated by Gradle 8.14.5 at `12.09.2026, 18:18:00`. Its test-results directory
contains only `binary/`, with no JUnit XML suite. This also occurred at baseline.
Gradle 8 explicitly warns that this will fail in Gradle 9. Root, api-spec, logic,
and translations test tasks are `NO-SOURCE`. Neither executed task names nor a
successful module build establish that those assertion specs ran. Test cases,
assertions and discovery wiring are unchanged; the pre-existing gap remains
reported, not fixed or hidden by the green build.

### Scope and Remaining Warnings

Changed files, and only these files:

- `gradle/wrapper/gradle-wrapper.properties`: 8.14.5 URL and generated `validateDistributionUrl=true`.
- `gradle/wrapper/gradle-wrapper.jar`: official Gradle 8.14.5 wrapper generated by the wrapper task.
- `gradlew`: generated Gradle 8.14.5 POSIX launcher.
- `gradlew.bat`: generated Gradle 8.14.5 Windows launcher; Windows execution not available on this Linux host.
- `build.gradle.kts`: one-line `reports.junitXml.required.set(true)` adaptation.
- `example-project/src/test/kotlin/KotlinPluginSpek.kt`: fixture-only Kotlin 1.4.10 to existing 1.9.25 alignment and the observed required `mainClass.set(...)` adaptation.
- `MODERNIZATION-PLAN.md`: restarted sequence, scope, official notes and final verification evidence.

`git diff --exit-code master -- .github gradle.properties settings.gradle.kts
api-spec apis logic translations example-project/build.gradle.kts` returned exit 0.
Root Kotlin, Atrium, Dokka, Nexus, Palantir and Spek versions, production code,
test assertions, publication architecture and CI are unchanged. Final execution
edited only the fixture DSL and this document; the wrapper, report DSL, and
fixture version alignment were inherited. No command in final execution touched
the original `/home/josh/Projekte/atrium-gradle-testkit` worktree. No commit,
push, PR, merge, publication, or persistent Git configuration change was made.

Plain `git diff --check` flagged the generated Windows launcher's CRLF endings
as trailing whitespace. `GIT_MASTER=1 git -c core.whitespace=cr-at-eol diff --check`
returned exit 0. The generated Windows line endings were preserved; no Git
configuration was persisted or changed.

The final warning-mode build reported deprecated convention access, JCenter,
`StartParameter.isConfigurationCacheRequested`, automatic framework loading,
and no fluent-en tests executed. Dokka reported unavailable Gradle `package-list`
URLs but generated all three documentation JARs. Earlier script-compilation
warnings about `capitalize`, `dependencyProject`, and `task`, and the earlier
daemon metaspace warning, remain historical evidence rather than new failures.
None was silently fixed or suppressed.

Kotlin LSP diagnostics for both changed Kotlin/KTS files (`build.gradle.kts` and
`example-project/src/test/kotlin/KotlinPluginSpek.kt`) returned no diagnostics.
No LSP is configured for wrapper properties, POSIX/Windows launchers, or Markdown;
the generated JAR is binary. Actual Gradle script compilation, CLI checks, shell
syntax checking, wrapper checksum verification, and file/diff review provide the
applicable validation. No LSP configuration or tool installation was changed.

---

## Kotlin 2.4.20 Evidence (2026-09-12)

### Version Selection and Official Sources

- [Kotlin 2.4.20 release notes](https://kotlinlang.org/docs/whatsnew2420.html): released September 7, 2026; the Gradle section lists full compatibility with Gradle 7.6.3 through 9.7.0. Gradle 8.14.5 is inside that range.
- [K2 migration guide](https://kotlinlang.org/docs/k2-compiler-migration-guide.html): K2 is the default from 2.0.0; upgrading to 2.0.0 or later enables it. Kotlin 2.4.0 onward cannot roll back to the previous compiler. No language-version rollback or compiler fallback was added.
- [KGP compatibility table](https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin): 2.4.20 supports Gradle 7.6.3-9.7.0. Listed 2.0 releases top out at 8.5/8.8 and listed 2.1 releases at 8.10/8.12.1, below this wrapper's 8.14.5. No unsupported 2.0/2.1 intermediate combinations were tested.
- [Compiler-options migration guide](https://kotlinlang.org/docs/gradle-compiler-options.html#migrate-from-kotlinoptions-to-compileroptions): replace deprecated `kotlinOptions` with typed `compilerOptions`; use `JvmTarget` instead of a raw target string and Gradle property/list APIs for options.

The direct 1.9.25 to 2.4.20 upgrade crosses one major-version boundary, 1 to 2.
The 2.0, 2.1, 2.2, 2.3, and 2.4 lines are within major version 2, not separate
major transitions required by the one-major-at-a-time agreement. The root PR
already proposed Kotlin 2; the generated TestKit build has its own independent
plugin literal, which must also be aligned.

### Reproduction and Minimal Adaptation

Before edits, `gradle.properties` still declared `atriumVersion=0.16.0`.
`git diff origin/master` (with `GIT_MASTER=1`) showed only the inherited root
plugin bump from 1.9.25 to 2.4.10. Java 17 `help` reproduced configuration errors
at root lines 57, 58, and 60 for `kotlinOptions`, string `jvmTarget`, and
list-style `freeCompilerArgs`, first with inherited 2.4.10 and again with 2.4.20.
The output also lists six existing deprecation warnings under its aggregate
`9 errors` heading; the three `e:` diagnostics identify the migration blockers.

Only the failing API was adapted: `compilerOptions`,
`jvmTarget.set(JvmTarget.JVM_1_8)`, and
`freeCompilerArgs.add("-Xno-optimized-callable-references")`, with the `JvmTarget`
import. The surrounding task configuration and Java 1.8 settings are unchanged.
The fixture's active plugin literal changed from 1.9.25 to 2.4.20; its
`mainClass.set(...)`, test body, and assertions are unchanged. The inactive
`/* version "1.4.10" */` comment in `example-project/build.gradle.kts` and
fluent-en's `explicitApi()` remain untouched.

### Exact Java 17 Commands and Results

Every Gradle command below ran in `/tmp/opencode/atrium-kotlin-2` with explicit
`JAVA_HOME=/home/linuxbrew/.linuxbrew/opt/sdkman-cli/libexec/candidates/java/17.0.20-tem`
and that installation's `bin` prepended to `PATH`. Logs are in
`/tmp/opencode/kotlin2-evidence/`; each new command log ends with `EXIT_CODE=N`.
Pipelines used `set -o pipefail`; no exclusions, ignored failures, suppression,
dependency substitutions, or compatibility fallbacks were supplied. The five
gates ran sequentially to prevent clean/build interference.

| Command | Exit Code | Observed Result | Log |
|---------|-----------|-----------------|-----|
| `./gradlew help --console=plain`, inherited 2.4.10 | 1 | Configuration failure, 11s, three removed-DSL errors | `red-inherited-2.4.10-help.log` |
| `./gradlew help --console=plain`, 2.4.20 before DSL migration | 1 | Same configuration failure, 15s | `red-2.4.20-help.log` |
| `./gradlew :example-project:test --rerun-tasks --console=plain` | 0 | 1m 7s; 9 tasks executed | `example-rerun.log` |
| `./gradlew clean test --console=plain` | 0 | 53s; 20 tasks executed | `clean-test.log` |
| `./gradlew clean build --warning-mode all --console=plain` | 0 | 1m 4s; 29 tasks executed, including all three documentation JARs | `clean-build.log` |
| `./gradlew checkDependenciesBeforePublishing --console=plain` | 0 | 5s; all 3 published-module checks executed | `check-dependencies.log` |
| `./gradlew release --dry-run --console=plain` | 0 | 1s; every task action skipped | `release-dry-run.log` |
| `./gradlew --version` | 0 | Gradle 8.14.5; launcher Java 17.0.20; daemon uses the requested Temurin installation | `gradle-version.log` |
| `./gradlew help -I /tmp/opencode/kotlin2-evidence/verify-targets.gradle --console=plain` | 0 | 1s; runtime target assertions pass for all five subprojects, main and test compilations | `target-alignment.log` |

The supplied `baseline-clean-test.log` remains unchanged; it records the earlier
master Java 17 baseline (`BUILD SUCCESSFUL in 1m 29s`, 24 actionable tasks).
Gradle's reported embedded Kotlin 2.0.21 is not the project's KGP version.

### Test Surface, Targets, and Release Graph

After the focused rerun, the example XML had exactly 1 test, 0 failures, 0 errors,
and 0 skips at `2026-09-12T17:12:01.150Z`. After the final clean build,
`example-project/build/test-results/test/TEST-KotlinPluginSpek.xml` again reports
`tests="1" skipped="0" failures="0" errors="0"`, timestamp
`2026-09-12T17:14:12.732Z`. The existing case, `compiles the Kotlin code and runs
it`, executes the real TestKit build's `:compileKotlin`, `:classes`, and `:run`,
prints `Hello World!`, and records `BUILD SUCCESSFUL in 48s` with empty stderr.
This is end-to-end execution through the existing assertions, not compile-only
validation.

`apis/fluent-en/build/reports/tests/test/index.html` still reports **0 tests,
0 failures, 0 ignored**, generated by Gradle 8.14.5 at `12.09.2026, 19:14:12`.
Its test-results directory contains only `binary/`, no JUnit XML suite. The
warning-mode build explicitly warns that this discovery gap will fail on
Gradle 9. Root, api-spec, logic, and translations tests are `NO-SOURCE`.
At the time, the zero-test gap was preserved and reported rather than fixed,
excluded, or hidden. The local Kotest migration documented below now resolves it.

The external, read-only target-verification init script asserts Java source and
target compatibility and Kotlin `jvmTarget` are all 1.8 for each subproject's
main/test compilation, with the callable-reference flag present. It also logs
`-Xexplicit-api=strict` for fluent-en's main compilation. Java 17 `javap -verbose`
on all six main class files across logic, fluent-en, and translations reports
major version **52**, Java 8 bytecode (`bytecode-targets.log`, exit 0).

The dry-run contains all three published paths:

- `:apis:atrium-gradle-testkit-fluent-en`
- `:atrium-gradle-testkit-logic`
- `:translations:atrium-gradle-testkit-translation-en`

Each retains dependency checks, source/Dokka JARs, Maven metadata/POM generation,
`initializeSonatypeStagingRepository`, `signMavenPublication`,
`publishMavenPublicationToSonatypeRepository`, `publishToSonatype`,
`publishMavenPublicationToGitHubPackagesRepository`,
`publishAllPublicationsToGitHubPackagesRepository`, and `release`.
Root `closeRepository`, `releaseRepository`, and `closeAndReleaseRepository`
are present. Dry-run verifies graph construction only, not signing credentials
or live repository compatibility. No signing or publication action executed.

### Scope and Remaining Warnings

Only `build.gradle.kts`, `example-project/src/test/kotlin/KotlinPluginSpek.kt`,
and this document changed. Comparison with `origin/master` confirms no changes
to `.github`, `gradle.properties`, settings, api-spec, fluent-en, logic,
translations, `example-project/build.gradle.kts`, the wrapper files, or Renovate
configuration. Atrium 0.16.0, Spek 2.0.17, spek-testfiles 1.0.3, Dokka 1.9.20,
Palantir 3.4.0, Nexus 0.4.0/0.30.0, Gradle 8.14.5, CI, production sources, tests
and assertions (other than the fixture version literal), and publication
architecture are unchanged. Kotlin's own runtime/compiler dependencies follow
the KGP upgrade; no unrelated dependency or tool version was edited.

Observed warnings remain for deprecated convention access, JCenter, automatic
test-framework loading, fluent-en's zero tests, the daemon's 384 MiB metaspace
limit, and unavailable Gradle Javadoc `package-list` URLs in Dokka. Existing
`capitalize`, `dependencyProject`, and `task` script warnings also remain.
None was suppressed or expanded into an unrelated cleanup.

Both changed Kotlin/KTS files returned no LSP diagnostics. The external Groovy
verification script has no configured LSP; actual Gradle execution passed.
`GIT_MASTER=1 git -c core.whitespace=cr-at-eol diff --check origin/master`
returned exit 0. No Git configuration was persisted. The superseded original
worktree was not touched or copied. Remote CI was not rerun; no commit, push,
merge, signing, or publication was performed.

### Independent Handoff Recheck (2026-09-12)

The worktree already contained the typed compiler block, Kotlin 2.4.20 root and
fixture literals, and the preceding evidence when this recheck began. Rather
than revert another execution's changes, a disposable standalone build at
`build/kotlin2-red-repro` reproduced the former `tasks.withType<KotlinCompile>`
block under KGP 2.4.20. It failed on exactly `kotlinOptions`, string `jvmTarget`,
and list `freeCompilerArgs`. Applying the same `JvmTarget` import and three
typed-option replacements made that identical build pass. This is a controlled
DSL reproduction, not a claim that the already-fixed root still failed.
The subsequent root `clean` task removed the disposable build, its generated
state, and its artifact journal. No production code or test assertions were
edited during the recheck.

All commands below ran sequentially in `/tmp/opencode/atrium-kotlin-2`, with
the explicit Java 17 `JAVA_HOME` and `PATH` from the preceding evidence section.
Each command used `set -o pipefail`, captured stdout and stderr with `tee`, and
appended its actual `EXIT_CODE` before returning that status. New logs use the
`recheck-` prefix in `/tmp/opencode/kotlin2-evidence/`; earlier logs are preserved.

| Command | Exit Code | Observed Result | Log |
|---------|-----------|-----------------|-----|
| `./gradlew -p build/kotlin2-red-repro help --console=plain`, legacy DSL | 1 | 7s; exactly 3 script compilation errors | `recheck-red-2.4.20.log` |
| Same command, typed DSL | 0 | 1s; 1 task executed | `recheck-green-2.4.20.log` |
| `./gradlew :example-project:test --rerun-tasks --console=plain` | 0 | 17s; 9 tasks executed | `recheck-example-rerun.log` |
| `./gradlew clean test --console=plain` | 0 | 52s; 20 tasks executed | `recheck-clean-test.log` |
| `./gradlew clean build --warning-mode all --console=plain` | 0 | 1m 1s; 29 tasks executed, including all three Dokka JARs | `recheck-clean-build.log` |
| `./gradlew checkDependenciesBeforePublishing --console=plain` | 0 | 632ms; all 3 published-module checks executed | `recheck-check-dependencies.log` |
| `./gradlew release --dry-run --console=plain` | 0 | 612ms; all task actions skipped | `recheck-release-dry-run.log` |
| `./gradlew --version` | 0 | Gradle 8.14.5; launcher/daemon Java 17.0.20 | `recheck-gradle-version.log` |

The focused example rerun produced XML timestamp `2026-09-12T17:50:45.739Z`
with exactly **1 test, 0 failures, 0 errors, 0 skips**. The final clean build
produced the same counts at `2026-09-12T17:52:29.412Z`. Its XML records the
existing test executing `:compileKotlin`, `:classes`, and `:run`, printing
`Hello World!`, and completing the nested build in 46s with empty stderr.
The extracted evidence is `recheck-example-xml.log`.

At the time, the fluent-en HTML still reported **0 tests, 0 failures, 0 ignored**, generated
at `12.09.2026, 19:52:29` (`recheck-fluent-en-report.log`). Its test-results
directory contains only `binary/`. The warning-mode build explicitly reports
that no tests executed and that this would fail on Gradle 9. That recheck did
not fix or suppress the gap. The local Kotest migration documented below now
resolves it. Convention access, JCenter, automatic framework loading, and
unavailable Dokka Gradle `package-list` warnings remained visible.

The release log again includes all three published module paths listed above.
For each it retains dependency checks, source/Dokka JARs, metadata/POM tasks,
Sonatype staging initialization, `signMavenPublication`, both Sonatype and
GitHub Packages publication tasks, and `release`. Root `closeRepository`,
`releaseRepository`, and `closeAndReleaseRepository` remain present. All are
`SKIPPED` by dry-run; this does not validate live signing or publication.
Java 17 `javap -verbose` on all six main class files again reports major version
52 (`recheck-bytecode-targets.log`, exit 0), preserving Java 8 bytecode.

The four official Kotlin sources above were fetched again: Kotlin 2.4.20 is
compatible with Gradle 7.6.3-9.7.0, K2 is the default, and the typed compiler
options are the documented migration. Direct 1.x to 2.x is one major transition;
unsupported intermediate KGP/Gradle combinations were not used in this recheck.

Both changed Kotlin/KTS files again returned no LSP diagnostics; Markdown has
no configured LSP. The CRLF-aware diff check against `origin/master` returned
exit 0 (`recheck-diff-check.log`). The protected-path comparison against that
base also returned exit 0. The only changed files remain `build.gradle.kts`,
`example-project/src/test/kotlin/KotlinPluginSpek.kt`, and this document.
The Kotlin/KTS files contain 187 and 59 nonblank/noncomment lines respectively.
No new helpers, boundary logic, fallbacks, logging, or assertions were added.
This recheck completed before commit or push. No publication or original-worktree operation was performed.

## Kotest Migration Evidence (2026-09-15)

The Java 17 baseline compiled the fluent test sources but executed zero fluent
tests, while the example executed one Spek test. The migration converted every
abstract and concrete suite to Kotest `FunSpec`, ported Atrium 0.16.0
`SubjectLessSpec` registration semantics into the project, and replaced
spek-testfiles with `Files.createTempDirectory` plus result-aware lifecycle
hooks. Each spec gets a unique directory; successful specs require cleanup to
succeed, while setup or test failures retain their fixture for diagnosis. No
test was removed, disabled, ignored, or otherwise suppressed, and Atrium
remains pinned at 0.16.0.

Scoped exclusions on `atrium-specs` remove transitive
`org.spekframework.spek2` and `ch.tutteli.spek` dependencies. `dependencyInsight`
for Spek reports no matching dependency on the migrated executable classpaths.
Kotest `dependencyInsight` selects JVM version 8 variants. Local Java 8 runtime
execution wasn't possible because this host has only JDK 17, 25, and 26
installed. At this stage, the existing CI Java 8, 11, and 16 matrix remained the
remote runtime gate.

All commands below ran sequentially with Java 17 in
`/tmp/opencode/atrium-kotest` and completed successfully.

| Command | Result |
|---------|--------|
| `./gradlew :apis:atrium-gradle-testkit-fluent-en:test :example-project:test --rerun-tasks --console=plain` | Passed in 15s. XML totals are 14 `BuildResult` plus 69 `BuildTask`, 83 fluent tests total, and 1 example test. Every suite reports zero failures, errors, and skips. |
| `./gradlew clean test --console=plain` | Passed in 55s. |
| `./gradlew clean build --warning-mode all --console=plain` | Passed in 1m 8s; all build tasks completed. |
| `./gradlew checkDependenciesBeforePublishing --console=plain` | Passed in 593ms for the dependency guard. |
| `./gradlew release --dry-run --console=plain` | Passed in 631ms. Every action was skipped and no publication occurred. |

Java 17 `javap -verbose` inspection of all six published main class files reports
class major version 52, preserving Java 8 bytecode. LSP diagnostics are clean
for every changed Kotlin and Kotlin DSL file. `GIT_MASTER=1 git diff --check`
passes.
Existing unrelated warnings remain for deprecated Gradle convention access,
JCenter, unavailable Dokka `package-list` resources, and daemon metaspace.
An independent read-only review found two low-severity cleanup regressions in
the first migration pass: unconditional deletion discarded failed fixtures and
unchecked `deleteRecursively()` could hide cleanup failure. Both are fixed by
tracking setup/test success and checking the deletion result; focused suites
still report 83 plus 1 passing tests after the correction.

At the time of this local evidence, no commit, push, PR, publication, signing
operation, CI run, or merge had occurred for the migration. It was subsequently
committed and merged through PR #166 as `4bee3c1` after Java 8/11/16 CI passed.
No publication or signing operation occurred as part of that prerequisite.

## Notes for Agents

- Use `sdk env` to select the Temurin 26 Gradle launcher from `.sdkmanrc`
- Omit `-PjavaVersion` for the default Java 17 toolchain; pass it explicitly for 25/26 checks
- Always run publication/release validation with `-PjavaVersion=17`
- Build file is Kotlin DSL (`build.gradle.kts`)
- Publishing is gated by `willBePublished` extra property
- Repository: `jGleitz/atrium-gradle-testkit`
- When updating this file, keep the phase status checkboxes current so the next agent knows where things stand.
