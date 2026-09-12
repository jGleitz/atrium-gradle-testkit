# atrium-gradle-testkit Modernization Plan

> **Coordinator**: Sisyphus (AI)
> **Last Updated**: 2026-09-12
> **Status**: Phase 0-1 merged. Isolated Gradle 8.14.5 bridge on `modernize/gradle-8` passes all four Java 17 gates. The example fixture uses the existing root Kotlin 1.9.25 version and the required `mainClass` DSL. No publication performed; fluent-en's existing zero-test gap remains.

This document persists the modernization work for this project. Any agent can pick up where the previous one left off by reading this file and the git history.

---

## Goal

Bring the `atrium-gradle-testkit` project up-to-date: modern Java, modern Gradle, modern Kotlin, replace Spek with kotest, and prepare for Atrium upgrade.

## Task List (from the original brief)

- [x] **Task 0**: Disable the GitHub build steps that would release the library
- [x] **Task 1**: Get the CI green again (Phase 0-1 merged in PR #162)
- [ ] **Task 2**: Get it to build with Java 26 (currently works with Java 17, not 26)
- [ ] **Task 3**: Update all libraries and tools **except Atrium** (Atrium update requires larger changes, deferred)
- [ ] **Task 4**: Migrate from Spek to kotest
- [ ] **Task 5**: Prepare an overview of architecture changes needed for latest Atrium (do NOT implement, just prepare information for next agent)

## Current State (validated Gradle 8 bridge)

| Component | Current Version | Notes |
|-----------|----------------|-------|
| Gradle | 8.14.5 | upgraded from 7.6.6; all four Java 17 gates pass |
| Kotlin | 1.9.25 | root unchanged; generated example fixture aligned from 1.4.10 to this existing version |
| Java | 8 target, 17 build | CI builds with 8/11/16 |
| Spek | 2.0.17 | test framework to be replaced by kotest |
| Atrium | 0.16.0 | **frozen** until other updates are done |
| spek-testfiles | 1.0.3 | jGleitz helper lib (may need replacement or update) |
| Dokka | 1.9.20 | docs |
| palantir git-version | 3.4.0 | versioning |
| nexus-publish / nexus-staging | 0.4.0 / 0.30.0 | unchanged plugins and publication architecture |
| CI | GitHub Actions | release steps disabled on master; action versions unchanged |

## Project Structure

```
├── api-spec/          # test utilities (spek, atrium-specs)
├── apis/fluent-en/    # the fluent Atrium API (published)
├── example-project/   # usage example with Spek
├── logic/             # core assertion logic (published)
├── translations/en/   # English translations (published)
└── build.gradle.kts   # root build (handles publishing, dokka, versioning)
```

Published modules: `apis:fluent-en`, `logic`, `translations:en` (via `willBePublished` extra flag).
Root project: releases to GitHub Packages + Sonatype/Maven Central via `nexus-publish`/`nexus-staging`, signed via Gradle signing plugin.

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
- **Status**: Merged. This session did not rerun remote CI or change any workflow.
- **Note**: `fluent-en` Spek tests currently silently run **0 tests** — a pre-existing issue to address in Phase 4 (Spek → kotest). Not a blocker for CI-green.

### Phase 2 - Restarted Sequence: Gradle 8 Before Gradle 9
- [x] Preserve the mixed `modernize/gradle-9` WIP without editing, cleaning, resetting, or copying its changes
- [x] Create `/tmp/opencode/atrium-gradle-8` on `modernize/gradle-8` from merged `master`
- [x] Select latest stable Gradle 8.x from official release metadata and read upgrade notes
- [x] Upgrade Gradle 7.6.6 to 8.14.5; adapt the removed report DSL and generated fixture application DSL
- [x] Execute all four required Java 17 checks and inspect actual test results
- [x] Resolve the fixture blocker using the authorized alignment to existing Kotlin 1.9.25; replace `mainClassName` only after observing its failure
- [x] Obtain green `clean test`, `clean build --warning-mode all`, `checkDependenciesBeforePublishing`, and `release --dry-run` before any commit
- [ ] After the Gradle 8 gate is green and approved, modernize remaining libraries individually, at most one major version per step
- [ ] Only then attempt Gradle 8 to 9 separately, followed by separately validated Java 26 configuration and CI changes
- **Status**: Gradle 8 bridge green on Java 17 and ready for review. Earlier combined Gradle 9/Kotlin/Nexus implementation and version conclusions remain superseded, not a basis for this branch.

### Phase 3 — Library Updates (one at a time, one PR each)
- Kotlin → latest
- Dokka → latest
- Spek-testfiles → replace/update
- Other build plugins (nexus-publish, git-version, etc.), one at a time; preserve publication architecture unless an explicit change is approved
- **Status**: Not started

### Phase 4 — Spek → kotest Migration
- Replace Spek DSL with kotest BehaviorSpec/StringSpec
- Update `api-spec` module (test utilities)
- Update `apis/fluent-en` tests
- Update `example-project` tests
- Include kotest-specific adaptations (e.g., `@DisplayName`, coroutines support)
- **Status**: Not started

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

`modernize/gradle-8` in `/tmp/opencode/atrium-gradle-8`, based on
`master` at `c95aaa2823209164572412da8e4d86251e739205` (PR #162).
The bridge was fully validated before its commits were created.

The original `/home/josh/Projekte/atrium-gradle-testkit` worktree remains on
`modernize/gradle-9` at `2985928`, with its seven dirty files preserved.
That mixed attempt is superseded by this isolated sequence. Do not clean it,
resume it, or copy its Kotlin/Nexus/dependency notation changes into this branch.

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

## Notes for Agents

- Use `sdk use java 17.0.20-tem` for current builds
- Use `sdk use java 26.0.1-tem` to test Java 26 compatibility
- Build file is Kotlin DSL (`build.gradle.kts`)
- Publishing is gated by `willBePublished` extra property
- Repository: `jGleitz/atrium-gradle-testkit`
- When updating this file, keep the phase status checkboxes current so the next agent knows where things stand.
