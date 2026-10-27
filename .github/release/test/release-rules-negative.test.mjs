import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { loadReleaseOptions, pluginConfig, releaseTypeOf } from "./support.mjs";

// Titles that the pull request title check rejects must not release either.
for (const message of [
	"Update dependencies",
	"fix the task outcome",
	"fix:missing space",
	"fix : space before colon",
	"Fix: capitalised type",
	"FIX: upper-case type",
	"FEAT: upper-case type",
	"feat(scope: unclosed scope",
	"feature: unknown type",
	"Merge branch 'main' into feature",
	'Revert "fix: report the actual task outcome"\n\nThis reverts commit 0123456789abcdef0123456789abcdef01234567.',
	"",
	"   ",
]) {
	test(`malformed or nonconventional message ${JSON.stringify(message)} releases nothing`, async () => {
		assert.equal(await releaseTypeOf(message), null);
	});
}

for (const message of [
	"fix(deps)!: update dependency ch.tutteli.atrium:atrium-logic to v2.0.0",
	"fix!: change failure reporting",
	"perf!: change output extraction",
	"chore(deps)!: drop Gradle 8 support",
	"fix(deps): update dependency ch.tutteli.atrium:atrium-logic to v2.0.0\n\nBREAKING CHANGE: Atrium 2 is required",
	"docs: describe the new API\n\nBREAKING CHANGE: old names were removed",
]) {
	test(`breaking marker in ${JSON.stringify(message)} never resolves to patch`, async () => {
		assert.equal(await releaseTypeOf(message), "major");
	});
}

test("breaking changes win over patches in the same release", async () => {
	assert.equal(
		await releaseTypeOf("fix(deps): update dependency x to v1.0.1", "feat!: rename API", "fix: y"),
		"major",
	);
});

test("analyzer and notes generator parse commits with the same preset", async () => {
	const options = await loadReleaseOptions();
	const analyzer = pluginConfig(options, "@semantic-release/commit-analyzer");
	const notes = pluginConfig(options, "@semantic-release/release-notes-generator");
	assert.equal(analyzer.preset, "conventionalcommits");
	assert.equal(notes.preset, analyzer.preset);
	for (const key of ["config", "parserOpts", "presetConfig"]) {
		assert.deepEqual(notes[key], analyzer[key], `${key} must match`);
	}
});

test("release tooling locks exactly the pinned tool versions", () => {
	const read = (file) => JSON.parse(readFileSync(new URL(`../${file}`, import.meta.url), "utf8"));
	const { devDependencies } = read("package.json");
	const { packages } = read("package-lock.json");
	assert.equal(devDependencies["conventional-changelog-conventionalcommits"], "9.3.1");
	for (const [name, version] of Object.entries(devDependencies)) {
		assert.match(version, /^\d+\.\d+\.\d+$/, `${name} must be pinned exactly`);
		assert.equal(packages[`node_modules/${name}`].version, version, `${name} must be locked at its pin`);
	}
});

// Deliberate: only the explicit rules release. Reverting released code needs a releasing type (e.g. `fix:`).
test("reverts release nothing", async () => {
	assert.equal(await releaseTypeOf("revert: fix: report the actual task outcome"), null);
	assert.equal(
		await releaseTypeOf(
			'revert: feat: add toHaveNoSource\n\nThis reverts commit 0123456789abcdef0123456789abcdef01234567.',
		),
		null,
	);
});
