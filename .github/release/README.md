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
