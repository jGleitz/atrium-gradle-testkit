import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { appendFileSync, mkdirSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { identity, verifyCandidate } from "./candidate.mjs";
import { github } from "./github.mjs";

export async function verifySource(release, api) {
	const ref = await api.json(`git/ref/tags/${release.tag}`);
	let object = ref.object;
	const tagObject = object.sha;
	// Annotated tags may point to other annotated tags. GitHub rejects cycles; bound malformed API responses anyway.
	for (let depth = 0; object.type === "tag"; depth++) {
		assert.ok(depth < 10, "tag chain too deep");
		object = (await api.json(`git/tags/${object.sha}`)).object;
	}
	assert.equal(object.type, "commit", "tag must resolve to a commit");
	assert.equal(object.sha, release.sha, "tag SHA mismatch");
	const comparison = await api.json(`compare/${release.sha}...main`);
	assert.equal(comparison.merge_base_commit.sha, release.sha, "release SHA is not on main");
	const runs = await api.pages(`actions/workflows/ci.yml/runs?head_sha=${release.sha}&event=push`, "workflow_runs");
	const matching = runs.filter((run) => run.head_sha === release.sha && run.head_branch === "main" && run.event === "push");
	const run = matching.sort((a, b) => b.id - a.id)[0];
	assert.ok(run && run.status === "completed" && run.conclusion === "success", "complete successful CI required");
	const jobs = await api.pages(`actions/runs/${run.id}/jobs?filter=latest`, "jobs");
	assert.ok(jobs.length > 0 && jobs.every((job) => job.status === "completed" && job.conclusion === "success"), "incomplete CI jobs");
	return { tagObject, ciRun: run.id, ciJobs: jobs.map((job) => job.name).sort() };
}

export async function preflight(env, api, download) {
	const release = identity(env);
	const environment = await api.json("environments/release-bootstrap");
	const review = environment.protection_rules.find((rule) => rule.type === "required_reviewers");
	// GitHub requires just one of the configured reviewers, so adding another reviewer would bypass owner approval.
	assert.ok(review?.reviewers.length === 1 &&
		review.reviewers[0].type === "User" &&
		review.reviewers[0].reviewer.login?.toLowerCase() === env.GITHUB_REPOSITORY.split("/")[0].toLowerCase(),
	"bootstrap environment must require owner review exclusively");
	assert.equal(review.prevent_self_review, false, "owner self-review must remain possible");
	assert.equal(environment.deployment_branch_policy?.custom_branch_policies, true, "explicit main restriction required");
	const policies = await api.pages("environments/release-bootstrap/deployment-branch-policies", "branch_policies");
	assert.deepEqual(policies.map(({ name, type }) => ({ name, type })), [{ name: "main", type: "branch" }], "bootstrap environment must allow main only");
	const source = await verifySource(release, api);
	if (env.EXPECTED_TAG_OBJECT) assert.equal(source.tagObject, env.EXPECTED_TAG_OBJECT, "tag was moved");
	if (env.EXPECTED_CI_RUN) assert.equal(String(source.ciRun), env.EXPECTED_CI_RUN, "CI run changed");
	const artifacts = await api.pages(`actions/runs/${source.ciRun}/artifacts`, "artifacts");
	const candidates = artifacts.filter((artifact) => artifact.name === `release-candidate-${release.sha}` && !artifact.expired);
	assert.equal(candidates.length, 1, "one unexpired candidate evidence bundle required");
	const directory = await download(source.ciRun, candidates[0].name);
	const candidate = verifyCandidate(directory, release);
	assert.deepEqual(source.ciJobs, [...candidate.manifest.ciJobs].sort(), "CI job inventory mismatch");
	if (env.EXPECTED_MANIFEST_HASH) assert.equal(candidate.manifestHash, env.EXPECTED_MANIFEST_HASH, "candidate evidence changed");
	return { ...candidate, ...source, directory };
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
	const env = process.env;
	const directory = resolve(env.RUNNER_TEMP, "bootstrap-candidate");
	const candidate = await preflight(env, github(env.GITHUB_REPOSITORY, env.GH_TOKEN), async (run, name) => {
		mkdirSync(directory);
		execFileSync("gh", ["run", "download", String(run), "--repo", env.GITHUB_REPOSITORY, "--name", name, "--dir", directory], {
			stdio: "inherit", timeout: 120_000,
		});
		return directory;
	});
	writeFileSync(resolve(env.RUNNER_TEMP, "bootstrap-preflight.json"), JSON.stringify(candidate, null, 2));
	for (const [key, value] of Object.entries({
		version: candidate.version, tag_object: candidate.tagObject, ci_run: candidate.ciRun, manifest_hash: candidate.manifestHash,
	})) appendFileSync(env.GITHUB_OUTPUT, `${key}=${value}\n`);
}
