# Protected first-v2 bootstrap

`release.yml` only accepts a manual dispatch **from main**, an existing exact stable `v<version>` tag and its full
commit SHA. It never creates a tag. All release jobs share `atrium-gradle-testkit-release` concurrency without
cancellation. The preflight uses read-only GitHub API calls and a read token, not production secrets.

Package 26 must separately obtain authorization and configure `release-bootstrap`: require the repository owner as
sole reviewer, allow owner self-review, and allow only the custom `main` branch deployment policy. Preflight checks these
settings before the environment job can start (a missing environment fails, rather than being implicitly created).
The environment needs `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `PACKAGE_SIGNING_KEY` (armored private key)
and `PACKAGE_SIGNING_KEY_PASSWORD`. `github.token` supplies GitHub Packages and release access. Pages must already
be configured for Actions deployment. This package does not configure any of those settings.

## Candidate interface for package 25

The latest **push-to-main** `ci.yml` run for the exact SHA must have completed successfully, with every job successful
(none skipped, cancelled or still running). It must upload exactly one unexpired artifact named
`release-candidate-<full SHA>`. The artifact contains this `candidate-manifest.json` schema:

```json
{
  "schemaVersion": 1,
  "sha": "<full SHA>",
  "tag": "v2.0.0",
  "version": "2.0.0",
  "notesReviewed": true,
  "warnings": [],
  "ciJobs": ["<every exact CI job name, including matrix suffixes>"],
  "gates": {
    "host": "gates/host.json",
    "abi": "gates/abi.json",
    "testkit8": "gates/testkit8.json",
    "testkit9": "gates/testkit9.json",
    "kotlinMinimum": "gates/kotlinMinimum.json",
    "kotlinCurrent": "gates/kotlinCurrent.json",
    "coverage8": "gates/coverage8.json",
    "coverage9": "gates/coverage9.json",
    "docs": "gates/docs.json",
    "releaseRules": "gates/releaseRules.json"
  },
  "artifacts": {
    "atrium-gradle-testkit-2.0.0.jar": "publication/atrium-gradle-testkit-2.0.0.jar",
    "atrium-gradle-testkit-2.0.0.pom": "publication/atrium-gradle-testkit-2.0.0.pom",
    "atrium-gradle-testkit-2.0.0.module": "publication/atrium-gradle-testkit-2.0.0.module",
    "atrium-gradle-testkit-2.0.0-sources.jar": "publication/atrium-gradle-testkit-2.0.0-sources.jar",
    "atrium-gradle-testkit-2.0.0-javadoc.jar": "publication/atrium-gradle-testkit-2.0.0-javadoc.jar"
  },
  "files": {
    "<every gate, publication, release-notes.md and live-preconditions.md path>": "<SHA-256>"
  }
}
```

Every gate JSON has `sha` and `status: "passed"`. Host, runtime and compiler gates also have positive `tests`, zero
`failures` and zero `skipped`; coverage gates have positive `instructions`, zero `missedInstructions` and zero
`missedBranches`. Package 25 owns the substantive evidence behind those receipts and records external warnings in
`warnings`. All referenced files must be nonempty and hash-match; files cannot escape the bundle. Notes are reviewed
data, never executed. The environment reviewer approves this exact bundle. After approval preflight repeats and pins
the tag object, CI run and manifest hash, detecting a moved tag, replaced evidence or new CI run.
Source identity and the CI run are checked once more after rebuilding, before destination reconciliation.

Release artifacts must be generated with matching `-Pversion=2.0.0 -PreleaseTag=v2.0.0` **without** publication approval,
using the production verification repository. The protected job rebuilds the same release and checks all five
publication hashes before enabling signing and publication. Central's signed release invocation finishes (including
its end-of-build PUBLISHED validation) before the separate GitHub Packages invocation, reviewed GitHub release and
Pages deployment of `library/build/dokka/html`. Only the Maven publishing step sets `remotePublicationApproved=true`.
Upload-only Vanniktech staging does not invoke the automatic-publishing task, so it does not wait for PUBLISHED.

## Same-version recovery

Re-dispatch the same tag/SHA, or re-run the failed job, after checking the partial destination state. Each destination
is read before any writes. GitHub Packages is read from the URL that the rebuild writes to
`build/release/github-packages-repository-url`, which is the URL Gradle publishes to (with the lowercase owner GitHub
Packages requires). A complete version must match all five candidate hashes and have signatures, then is
skipped. A partial version or differing hash stops for reconciliation; no immutable Central artifact is overwritten.
Existing GitHub releases must have the same SHA and exact reviewed notes and are left alone. Pages is redeployed.

The workflow inspects **all attempts** of earlier bootstrap runs for the tag, including failed/cancelled attempts.
If a Maven publishing step already started but Central is not yet completely visible, recovery stops rather than
guessing whether Portal accepted the upload. Wait for visibility/reconcile the actual Portal deployment under the
package 26 authorization, then run the same version again. Do not pick another version, delete a deployment or edit
a release automatically. GitHub API errors and timeouts fail closed; only HTTP 404 means an absent artifact.

Local checks: `mise exec -- npm test` in this directory runs the preflight and recovery fixtures with the release-rule
tests. They do not validate the live Portal, credentials or the production signing identity.

## Release tooling dependencies

Private, unpublished npm package that pins the semantic-release tooling used for automatic releases (configuration:
`.releaserc.yaml` in the repository root) and holds the tests of the release rules.

```sh
mise exec node@24 -- npm ci --prefix .github/release
mise exec node@24 -- npm --prefix .github/release test
```

Direct dependencies are pinned to exact versions in `package.json`, everything else by `package-lock.json`. Renovate
updates both.

### Overrides

None. Findings with a fixed release inside the pinned majors are resolved by the lockfile alone: the `npm` that
`@semantic-release/npm` depends on is locked at the newest 11.x release (its range is `^11.6.2`), which fixed the
`undici` and `ip-address` advisories.

Add an `overrides` entry only together with a note in this section that says why it exists and when to remove it.
`package.json` cannot hold comments.

### Known `npm audit` findings

`npm --prefix .github/release audit` reports 41 findings (10 high, 31 moderate). They stem from the three advisories
below, which have no usable fix. Nothing is suppressed. All findings are development dependencies of the release job; none
ships in the published library.

The release run does execute picomatch, but none of the findings is exploitable through the configured release inputs:

- picomatch runs during branch matching and commit analysis, but it only ever compiles the fixed patterns from
  `.releaserc.yaml` (`feat`, `fix`, `deps`, `perf`, `main`), which is reviewed repository content. Commit messages, branch
  names and tags are matched as input, never compiled as patterns. `@semantic-release/github` is configured without
  `assets`, so it globs nothing.
- minimatch and glob only exist inside npm, a dependency of the unused `@semantic-release/npm` plugin, which bundles
  them. `.releaserc.yaml` lists its plugins explicitly, so that plugin is not loaded and npm is never executed.

| Advisory | Where | Why it cannot be fixed now | Remove when |
| --- | --- | --- | --- |
| [GHSA-fc5q-39xc-vjpf](https://github.com/advisories/GHSA-fc5q-39xc-vjpf), picomatch ReDoS, `<=4.0.7` (high) | `picomatch` 2.3.2 via `micromatch` (semantic-release, commit-analyzer); 4.0.7 via `tinyglobby` (`@semantic-release/github`); bundled copies in `npm`. Raises `micromatch`, `tinyglobby`, `fdir`, `node-gyp`, `@npmcli/run-script`, `npm`, `@semantic-release/*` and `semantic-release` itself | Every picomatch release, including the newest (4.0.7), is affected. npm's suggested fix downgrades semantic-release to 15.x. | A picomatch release above 4.0.7 exists and the dependents accept it; then `npm update` clears it. |
| [GHSA-g3hv-75wg-m249](https://github.com/advisories/GHSA-g3hv-75wg-m249), minimatch DoS, `>=7.0.0 <11.0.1` (moderate) | `minimatch` 10.2.5 bundled in `npm`, and every npm package that depends on it (`@npmcli/*`, `libnpm*`, `cacache`, `pacote`, `sigstore`, ...) | The fix is minimatch 11, a major above what the bundled npm 11.21.3 uses. Dependencies bundled in the npm tarball ignore `overrides`. | A release of `npm` 11.x bundles minimatch >=11.0.1 and `npm update npm` picks it up. |
| [GHSA-3g3m-2fjr-6pw4](https://github.com/advisories/GHSA-3g3m-2fjr-6pw4), glob AbortSignal bypass, `>=9.0.0 <=14.0.1` (moderate) | `glob` 13.0.6 bundled in `npm` | Same as minimatch: the fix is glob >=14.0.2 and cannot be overridden inside the bundle. | A release of `npm` 11.x bundles glob >=14.0.2. |

Check these conditions when updating the tooling. `npm audit` itself lists them again once they are fixed.
