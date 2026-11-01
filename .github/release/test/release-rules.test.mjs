import assert from "node:assert/strict";
import { cpSync } from "node:fs";
import { join } from "node:path";
import { test } from "node:test";
import semanticRelease from "semantic-release";
import {
	discard,
	git,
	loadReleaseOptions,
	pluginConfig,
	releaseNotesOf,
	releaseTypeOf,
	repositoryRoot,
	temporaryDirectory,
} from "./support.mjs";

test("production dependency updates release a patch", async () => {
	assert.equal(await releaseTypeOf("fix(deps): update dependency ch.tutteli.atrium:atrium-logic to v1.3.1"), "patch");
});

test("fixes and performance improvements release a patch", async () => {
	assert.equal(await releaseTypeOf("fix: report the actual task outcome"), "patch");
	assert.equal(await releaseTypeOf("perf(output): avoid copying the build output"), "patch");
});

test("features release a minor version", async () => {
	assert.equal(await releaseTypeOf("feat: add toHaveNoSource"), "minor");
	assert.equal(await releaseTypeOf("chore: tidy up", "feat(task): add task expectations", "fix: x"), "minor");
});

test("breaking header marker releases a major version", async () => {
	assert.equal(await releaseTypeOf("feat!: rename the expectation API"), "major");
});

test("breaking change footer releases a major version", async () => {
	assert.equal(
		await releaseTypeOf("feat: rename the expectation API\n\nBREAKING CHANGE: old names were removed"),
		"major",
	);
	assert.equal(await releaseTypeOf("refactor: drop module\n\nBREAKING-CHANGE: module removed"), "major");
});

for (const type of ["chore", "docs", "style", "refactor", "test", "build", "ci", "revert"]) {
	test(`${type} commits release nothing`, async () => {
		assert.equal(await releaseTypeOf(`${type}: change something`, `${type}(scope): change more`), null);
	});
}

test("non-production dependency updates release nothing", async () => {
	assert.equal(
		await releaseTypeOf(
			"chore(deps): update dependency io.kotest:kotest-runner-junit5 to v6.2.7",
			"chore(deps): update actions/checkout action to v7.0.2",
			"chore(deps): lock file maintenance",
		),
		null,
	);
});

test("release notes contain releasing changes and omit housekeeping", async () => {
	const notes = await releaseNotesOf(
		"2.1.0",
		"feat: add toHaveNoSource",
		"fix(deps): update dependency ch.tutteli.atrium:atrium-logic to v1.3.1",
		"chore(deps): update dependency io.kotest:kotest-runner-junit5 to v6.2.7",
	);
	assert.match(notes, /2\.1\.0/);
	assert.match(notes, /add toHaveNoSource/);
	assert.match(notes, /\*\*deps:\*\* update dependency ch\.tutteli\.atrium:atrium-logic to v1\.3\.1/);
	assert.doesNotMatch(notes, /kotest-runner-junit5/);
});

test("release notes list breaking changes", async () => {
	const notes = await releaseNotesOf("3.0.0", "feat!: rename the expectation API");
	assert.match(notes, /BREAKING CHANGES/);
	assert.match(notes, /rename the expectation API/);
});

test("configuration releases v-prefixed versions from main and publishes through Gradle", async () => {
	const options = await loadReleaseOptions();
	assert.deepEqual(options.branches, ["main"]);
	assert.equal(options.tagFormat, "v${version}");
	assert.deepEqual(
		options.plugins.map((plugin) => (Array.isArray(plugin) ? plugin[0] : plugin)),
		[
			"@semantic-release/commit-analyzer",
			"@semantic-release/release-notes-generator",
			"@semantic-release/exec",
			"@semantic-release/github",
		],
	);
	const exec = pluginConfig(options, "@semantic-release/exec");
	assert.doesNotMatch(exec.prepareCmd, /remotePublicationApproved/);
	for (const property of ["-Pversion=${nextRelease.version}", "-PreleaseTag=v${nextRelease.version}"]) {
		assert.ok(exec.prepareCmd.includes(property), `prepareCmd passes ${property}`);
		assert.ok(exec.publishCmd.includes(property), `publishCmd passes ${property}`);
	}
	assert.ok(exec.publishCmd.includes("-PremotePublicationApproved=true"));
	assert.match(exec.publishCmd, /\bpublishAndReleaseToMavenCentral\b/);
	assert.match(exec.publishCmd, /\bpublishAllPublicationsToGithubPackagesRepository\b/);
	assert.deepEqual(pluginConfig(options, "@semantic-release/github"), {
		successComment: false,
		failComment: false,
		releasedLabels: false,
	});
});

test("a v2.0.0 baseline plus a production dependency update selects 2.0.1", async (t) => {
	const directory = temporaryDirectory(t);
	const remote = join(directory, "remote.git");
	const checkout = join(directory, "checkout");
	git(directory, "init", "--quiet", "--bare", "--initial-branch=main", remote);
	git(directory, "clone", "--quiet", remote, checkout);
	cpSync(join(repositoryRoot, ".releaserc.yaml"), join(checkout, ".releaserc.yaml"));
	git(checkout, "add", ".releaserc.yaml");
	git(checkout, "-c", "commit.gpgsign=false", "commit", "--quiet", "-m", "feat: first v2 release");
	git(checkout, "tag", "v2.0.0");
	git(checkout, "-c", "commit.gpgsign=false", "commit", "--quiet", "--allow-empty", "-m", "docs: explain the API");
	git(
		checkout,
		"-c",
		"commit.gpgsign=false",
		"commit",
		"--quiet",
		"--allow-empty",
		"-m",
		"fix(deps): update dependency ch.tutteli.atrium:atrium-logic to v1.3.1",
	);
	git(checkout, "push", "--quiet", "origin", "main", "v2.0.0");

	const options = await loadReleaseOptions();
	const result = await semanticRelease(
		{
			dryRun: true,
			ci: false,
			repositoryUrl: `file://${remote}`,
			plugins: [
				["@semantic-release/commit-analyzer", pluginConfig(options, "@semantic-release/commit-analyzer")],
				[
					"@semantic-release/release-notes-generator",
					pluginConfig(options, "@semantic-release/release-notes-generator"),
				],
			],
		},
		{
			cwd: checkout,
			env: { PATH: process.env.PATH, HOME: directory, GIT_CONFIG_NOSYSTEM: "1", GIT_CONFIG_GLOBAL: "/dev/null" },
			stdout: discard(),
			stderr: discard(),
		},
	);

	assert.ok(result, "semantic-release must select a release");
	assert.equal(result.lastRelease.version, "2.0.0");
	assert.equal(result.nextRelease.type, "patch");
	assert.equal(result.nextRelease.version, "2.0.1");
	assert.equal(result.nextRelease.gitTag, "v2.0.1");
	assert.match(result.nextRelease.notes, /update dependency ch\.tutteli\.atrium:atrium-logic to v1\.3\.1/);
	assert.equal(git(checkout, "tag", "--list").trim(), "v2.0.0", "a dry run must not create tags");
});
