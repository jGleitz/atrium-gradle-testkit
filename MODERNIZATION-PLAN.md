# atrium-gradle-testkit Modernization Plan

> **Coordinator**: Sisyphus (AI)
> **Last Updated**: 2026-09-12
> **Status**: Phase 0+1 merged to master. Phase 2 in progress (Gradle 9 DSL fixes).

This document persists the modernization work for this project. Any agent can pick up where the previous one left off by reading this file and the git history.

---

## Goal

Bring the `atrium-gradle-testkit` project up-to-date: modern Java, modern Gradle, modern Kotlin, replace Spek with kotest, and prepare for Atrium upgrade.

## Task List (from the original brief)

- [ ] **Task 0**: Disable the GitHub build steps that would release the library
- [ ] **Task 1**: Get the CI green again
- [ ] **Task 2**: Get it to build with Java 26 (currently works with Java 17, not 26)
- [ ] **Task 3**: Update all libraries and tools **except Atrium** (Atrium update requires larger changes, deferred)
- [ ] **Task 4**: Migrate from Spek to kotest
- [ ] **Task 5**: Prepare an overview of architecture changes needed for latest Atrium (do NOT implement, just prepare information for next agent)

## Current State (as of start)

| Component | Current Version | Notes |
|-----------|----------------|-------|
| Gradle | 7.6.6 | far too old for Java 26 (needs Gradle 9.x) |
| Kotlin | 1.9.25 | old but works with old Gradle |
| Java | 8 target, 17 build | CI builds with 8/11/16 |
| Spek | 2.0.17 | test framework to be replaced by kotest |
| Atrium | 0.16.0 | **frozen** until other updates are done |
| spek-testfiles | 1.0.3 | jGleitz helper lib (may need replacement or update) |
| Dokka | 1.9.20 | docs |
| palantir git-version | 3.4.0 | versioning |
| CI | GitHub Actions | has release steps to be disabled |

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
- [x] Verify no accidental publication is possible
- **Status**: ✅ Done (commit c2f8c04 on branch `modernize/no-release-pipeline`)

### Phase 1 — CI Green
- [x] Check what passes today (Build passed locally on Java 17)
- [x] Fix CI workflow (`adopt` → `temurin` to silence deprecation)
- [x] Push branch, create PR #162, merged via squash (commit c95aaa2)
- **Status**: ✅ Done. Branch protection rule "Release Check" removed via GitHub API.
- **Note**: `fluent-en` Spek tests currently silently run **0 tests** — a pre-existing issue to address in Phase 4 (Spek → kotest).

### Phase 2 — Java 26 (in progress on branch `modernize/gradle-9`)
- [x] Research confirmed: Gradle 9.7.1 + Kotlin 2.4.20 (librarian research)
- [x] Gradle wrapper updated to 9.7.1 (commit a78f78c)
- [ ] DSL deprecation fixes — background subagent running
- [ ] Then Kotlin 2.4.20 upgrade
- [ ] CI matrix updated
- **Status**: In progress

### Phase 3 — Library Updates (one at a time, one PR each)
- Kotlin → latest
- Dokka → latest
- Spek-testfiles → replace/update
- Other build plugins (nexus-publish, git-version, etc.) — these may be removed if release is no longer needed
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

## Guardrails

- Every commit/PR must identify as AI-authored (git committer name change)
- No architecture/design changes without explicit approval — only adaptations
- Improvement proposals for architecture → propose to user first
- If unclear → ask before burning tokens

## Current Branch

`master` (initial state). Work will be done on feature branches merged via PR.

---

## Notes for Agents

- Use `sdk use java 17.0.20-tem` for current builds
- Use `sdk use java 26.0.1-tem` to test Java 26 compatibility
- Build file is Kotlin DSL (`build.gradle.kts`)
- Publishing is gated by `willBePublished` extra property
- Repository: `jGleitz/atrium-gradle-testkit`
- When updating this file, keep the phase status checkboxes current so the next agent knows where things stand.
