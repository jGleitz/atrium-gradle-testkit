import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { Writable } from "node:stream";
import { fileURLToPath } from "node:url";
import { analyzeCommits } from "@semantic-release/commit-analyzer";
import { generateNotes } from "@semantic-release/release-notes-generator";
import getConfig from "semantic-release/lib/get-config.js";

export const repositoryRoot = fileURLToPath(new URL("../../../", import.meta.url));

export const silentLogger = { log() {}, info() {}, warn() {}, error() {}, success() {} };
export const discard = () =>
	new Writable({
		write(_chunk, _encoding, callback) {
			callback();
		},
	});

/** Loads the repository's release configuration exactly as semantic-release does. */
export async function loadReleaseOptions() {
	const { options } = await getConfig(
		{ cwd: repositoryRoot, env: {}, logger: silentLogger, stdout: discard(), stderr: discard() },
		{},
	);
	return options;
}

export function pluginConfig(options, pluginName) {
	const entry = options.plugins.find((plugin) => (Array.isArray(plugin) ? plugin[0] : plugin) === pluginName);
	assert.ok(entry, `${pluginName} must be configured`);
	return Array.isArray(entry) ? (entry[1] ?? {}) : {};
}

const toCommits = (messages) =>
	messages.map((message, index) => ({
		message,
		hash: index.toString(16).padStart(40, "0"),
		committerDate: "2026-10-27T00:00:00Z",
	}));

/** The release type that the configured, pinned commit analyzer selects for the given commit messages. */
export async function releaseTypeOf(...messages) {
	const options = await loadReleaseOptions();
	return analyzeCommits(pluginConfig(options, "@semantic-release/commit-analyzer"), {
		commits: toCommits(messages),
		logger: silentLogger,
		cwd: repositoryRoot,
	});
}

/** The release notes that the configured, pinned notes generator creates for the given commit messages. */
export async function releaseNotesOf(version, ...messages) {
	const options = await loadReleaseOptions();
	return generateNotes(pluginConfig(options, "@semantic-release/release-notes-generator"), {
		commits: toCommits(messages),
		lastRelease: { gitTag: "v2.0.0", version: "2.0.0" },
		nextRelease: { gitTag: `v${version}`, version },
		options: { repositoryUrl: "https://github.com/jGleitz/atrium-gradle-testkit" },
		logger: silentLogger,
		cwd: repositoryRoot,
	});
}

/** A temporary directory that is removed after the test. */
export function temporaryDirectory(testContext) {
	const directory = mkdtempSync(join(tmpdir(), "atrium-gradle-testkit-release-"));
	testContext.after(() => rmSync(directory, { recursive: true, force: true }));
	return directory;
}

/** Runs git in isolation from the user's and system's git configuration. */
export function git(cwd, ...args) {
	return execFileSync("git", args, {
		cwd,
		encoding: "utf8",
		env: {
			PATH: process.env.PATH,
			HOME: cwd,
			GIT_CONFIG_NOSYSTEM: "1",
			GIT_CONFIG_GLOBAL: "/dev/null",
			GIT_AUTHOR_NAME: "Release Test",
			GIT_AUTHOR_EMAIL: "release-test@noreply",
			GIT_COMMITTER_NAME: "Release Test",
			GIT_COMMITTER_EMAIL: "release-test@noreply",
		},
	});
}
