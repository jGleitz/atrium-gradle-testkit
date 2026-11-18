import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { lstatSync, readFileSync, realpathSync } from "node:fs";
import { resolve, sep } from "node:path";

export const gates = [
	"host", "abi", "testkit8", "testkit9", "kotlinMinimum", "kotlinCurrent",
	"coverage8", "coverage9", "docs", "releaseRules",
];
export const digest = (bytes) => createHash("sha256").update(bytes).digest("hex");

export function identity(env) {
	assert.equal(env.GITHUB_EVENT_NAME, "workflow_dispatch", "manual dispatch required");
	assert.equal(env.GITHUB_REF, "refs/heads/main", "dispatch must use main");
	assert.match(env.RELEASE_TAG ?? "", /^v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/, "stable tag required");
	assert.match(env.RELEASE_SHA ?? "", /^[0-9a-f]{40}$/, "full immutable commit SHA required");
	assert.match(env.GITHUB_REPOSITORY ?? "", /^[\w.-]+\/[\w.-]+$/, "repository required");
	assert.equal(env.ORG_GRADLE_PROJECT_remotePublicationApproved, undefined, "premature publication approval");
	return { tag: env.RELEASE_TAG, sha: env.RELEASE_SHA, version: env.RELEASE_TAG.slice(1) };
}

/** Candidate files are data, never executable code. Reject traversal and symlinks escaping the bundle. */
export function candidateFile(directory, path) {
	assert.equal(typeof path, "string", "candidate path required");
	assert.match(path, /^[A-Za-z0-9_./-]+$/, "unsafe candidate path");
	assert.ok(!path.startsWith("/") && !path.split("/").some((part) => ["", ".", ".."].includes(part)), "relative path required");
	const file = resolve(directory, path);
	assert.ok(realpathSync(file).startsWith(realpathSync(directory) + sep), "candidate path escapes bundle");
	assert.ok(lstatSync(file).isFile(), "candidate entry must be a regular file");
	return file;
}

export function artifactNames(version) {
	const prefix = `atrium-gradle-testkit-${version}`;
	return [`${prefix}.jar`, `${prefix}.pom`, `${prefix}.module`, `${prefix}-sources.jar`, `${prefix}-javadoc.jar`];
}

export function verifyCandidate(directory, release) {
	const raw = readFileSync(candidateFile(directory, "candidate-manifest.json"));
	const manifest = JSON.parse(raw);
	assert.equal(manifest.schemaVersion, 1);
	for (const key of ["sha", "version", "tag"]) assert.equal(manifest[key], release[key], `candidate ${key} mismatch`);
	assert.equal(manifest.notesReviewed, true, "reviewed release notes required");
	assert.ok(Array.isArray(manifest.warnings), "warning disposition required");
	assert.ok(Array.isArray(manifest.ciJobs) && manifest.ciJobs.length > 0, "expected CI jobs required");
	assert.equal(new Set(manifest.ciJobs).size, manifest.ciJobs.length, "duplicate CI job");
	for (const [path, hash] of Object.entries(manifest.files)) {
		assert.match(hash, /^[a-f0-9]{64}$/, "SHA-256 required");
		const bytes = readFileSync(candidateFile(directory, path));
		assert.ok(bytes.length > 0, `empty evidence: ${path}`);
		assert.equal(digest(bytes), hash, `candidate hash mismatch: ${path}`);
	}
	for (const gate of gates) {
		const path = manifest.gates[gate];
		assert.ok(Object.hasOwn(manifest.files, path), `missing ${gate} evidence`);
		const result = JSON.parse(readFileSync(candidateFile(directory, path), "utf8"));
		assert.equal(result.sha, release.sha, `${gate} evidence SHA mismatch`);
		assert.equal(result.status, "passed", `${gate} gate failed`);
		if (["host", "testkit8", "testkit9", "kotlinMinimum", "kotlinCurrent"].includes(gate)) {
			assert.ok(result.tests > 0 && result.failures === 0 && result.skipped === 0, `${gate} incomplete tests`);
		}
		if (gate.startsWith("coverage")) {
			assert.ok(result.instructions > 0 && result.missedInstructions === 0 && result.missedBranches === 0, `${gate} incomplete coverage`);
		}
	}
	for (const path of ["release-notes.md", "live-preconditions.md"]) {
		assert.ok(Object.hasOwn(manifest.files, path), `missing ${path}`);
	}
	const names = artifactNames(release.version);
	assert.deepEqual(Object.keys(manifest.artifacts).sort(), [...names].sort(), "exact neutral publication required");
	for (const path of Object.values(manifest.artifacts)) {
		assert.ok(Object.hasOwn(manifest.files, path), `missing artifact hash: ${path}`);
	}
	return { ...release, manifest, manifestHash: digest(raw) };
}
