import assert from "node:assert/strict";
import { appendFileSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { verifySource } from "./bootstrap.mjs";
import { candidateFile, digest, verifyCandidate } from "./candidate.mjs";
import { get, github } from "./github.mjs";

export function verifySecrets(env) {
	for (const name of ["MAVEN_CENTRAL_USERNAME", "MAVEN_CENTRAL_PASSWORD", "PACKAGE_SIGNING_KEY", "PACKAGE_SIGNING_KEY_PASSWORD", "GH_TOKEN"]) {
		assert.ok(env[name]?.trim(), `missing secret: ${name}`);
	}
	assert.equal(env.ORG_GRADLE_PROJECT_remotePublicationApproved, undefined, "premature publication approval");
}

/** Returns true only for an absent version; never re-upload a complete or partially visible Central version. */
export async function needsPublication(candidate, readRemote) {
	const artifacts = Object.entries(candidate.manifest.artifacts);
	let present = 0;
	for (const [name, path] of artifacts) {
		const bytes = await readRemote(name);
		if (bytes === null) continue;
		present++;
		assert.equal(digest(bytes), candidate.manifest.files[path], `immutable artifact mismatch: ${name}`);
		const signature = await readRemote(`${name}.asc`);
		assert.ok(signature?.length > 0, `missing release signature: ${name}`);
	}
	assert.ok(present === 0 || present === artifacts.length, "partially visible destination; retry only after reconciliation");
	return present === 0;
}

export async function previousCentralAttempt(candidate, api) {
	const runs = await api.pages("actions/workflows/release.yml/runs?event=workflow_dispatch", "workflow_runs");
	for (const run of runs.filter((entry) => entry.display_title.startsWith(`Bootstrap ${candidate.tag} at `))) {
		const jobs = await api.pages(`actions/runs/${run.id}/jobs?filter=all`, "jobs");
		if (jobs.some((job) => job.steps?.some((step) =>
			step.name === "Publish Maven artifacts" &&
			["in_progress", "completed"].includes(step.status) && step.conclusion !== "skipped",
		))) return true;
	}
	return false;
}

export function verifyStaged(candidate, repository) {
	for (const [name, path] of Object.entries(candidate.manifest.artifacts)) {
		const file = resolve(repository, "de/joshuagleitze/atrium-gradle-testkit", candidate.version, name);
		assert.equal(digest(readFileSync(file)), candidate.manifest.files[path], `rebuilt artifact differs from candidate: ${name}`);
	}
}

export async function recoveryPlan(candidate, services) {
	// Rebuilding can take long enough for a tag move or CI rerun. Pin the approved source again before any write.
	const source = await verifySource(candidate, services.api);
	assert.equal(source.tagObject, candidate.tagObject, "tag was moved during rebuild");
	assert.equal(source.ciRun, candidate.ciRun, "CI run changed during rebuild");
	assert.deepEqual(source.ciJobs, [...candidate.manifest.ciJobs].sort(), "CI jobs changed during rebuild");
	const central = await needsPublication(candidate, services.central);
	if (central) assert.equal(await previousCentralAttempt(candidate, services.api), false, "previous Central attempt has uncertain state; do not upload again");
	const packages = await needsPublication(candidate, services.packages);
	return { central, packages };
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
	const env = process.env;
	verifySecrets(env);
	const receipt = JSON.parse(readFileSync(resolve(env.RUNNER_TEMP, "bootstrap-preflight.json"), "utf8"));
	const candidate = { ...receipt, ...verifyCandidate(receipt.directory, receipt) };
	verifyStaged(candidate, resolve("build/verification-repository"));
	const prefix = `de/joshuagleitze/atrium-gradle-testkit/${candidate.version}/`;
	const api = github(env.GITHUB_REPOSITORY, env.GH_TOKEN);
	const plan = await recoveryPlan(candidate, {
		api,
		central: (name) => get(`https://repo.maven.apache.org/maven2/${prefix}${name}`),
		packages: (name) => get(
			`https://maven.pkg.github.com/${env.GITHUB_REPOSITORY.toLowerCase()}/${prefix}${name}`,
			`Basic ${Buffer.from(`${env.GITHUB_ACTOR}:${env.GH_TOKEN}`).toString("base64")}`,
		),
	});
	const notes = readFileSync(candidateFile(receipt.directory, "release-notes.md"), "utf8");
	const existing = await get(`https://api.github.com/repos/${env.GITHUB_REPOSITORY}/releases/tags/${candidate.tag}`, `Bearer ${env.GH_TOKEN}`);
	if (existing) {
		const release = JSON.parse(existing.toString("utf8"));
		assert.equal(release.draft, false, "an existing draft needs explicit reconciliation");
		assert.equal(release.prerelease, false);
		assert.equal(release.target_commitish, candidate.sha, "release commit mismatch");
		assert.equal(release.body, notes, "published release notes differ");
	}
	for (const [key, value] of Object.entries({ ...plan, release: !existing })) {
		appendFileSync(env.GITHUB_OUTPUT, `${key}=${value}\n`);
	}
}
