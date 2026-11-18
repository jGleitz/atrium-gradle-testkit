import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { artifactNames, digest, gates } from "../candidate.mjs";

export function fixture(t) {
	const directory = mkdtempSync(join(tmpdir(), "bootstrap-"));
	t.after(() => rmSync(directory, { recursive: true, force: true }));
	const sha = "a".repeat(40);
	const release = { sha, version: "2.0.0", tag: "v2.0.0" };
	const env = {
		GITHUB_EVENT_NAME: "workflow_dispatch", GITHUB_REF: "refs/heads/main",
		GITHUB_REPOSITORY: "owner/repository", RELEASE_TAG: release.tag, RELEASE_SHA: sha,
	};
	const manifest = {
		schemaVersion: 1, ...release, notesReviewed: true, warnings: [], ciJobs: ["Build (21)", "CI Complete"],
		gates: {}, files: {}, artifacts: {},
	};
	const put = (path, value) => {
		writeFileSync(join(directory, path), value);
		manifest.files[path] = digest(value);
	};
	mkdirSync(join(directory, "gates"));
	for (const gate of gates) {
		const path = `gates/${gate}.json`;
		manifest.gates[gate] = path;
		put(path, JSON.stringify({ sha, status: "passed", tests: 1, failures: 0, skipped: 0, instructions: 1, missedInstructions: 0, missedBranches: 0 }));
	}
	put("release-notes.md", "Reviewed fixture notes\n");
	put("live-preconditions.md", "Production credentials are not validated locally\n");
	for (const name of artifactNames(release.version)) {
		manifest.artifacts[name] = name;
		put(name, `candidate ${name}`);
	}
	const save = () => writeFileSync(join(directory, "candidate-manifest.json"), JSON.stringify(manifest));
	save();
	const routes = new Map([
		["environments/release-bootstrap", {
			protection_rules: [{ type: "required_reviewers", prevent_self_review: false, reviewers: [{ type: "User", reviewer: { login: "owner" } }] }],
			deployment_branch_policy: { custom_branch_policies: true },
		}],
		["environments/release-bootstrap/deployment-branch-policies", [{ name: "main", type: "branch" }]],
		[`git/ref/tags/${release.tag}`, { object: { type: "tag", sha: "b".repeat(40) } }],
		[`git/tags/${"b".repeat(40)}`, { object: { type: "commit", sha } }],
		[`compare/${sha}...main`, { merge_base_commit: { sha } }],
		[`actions/workflows/ci.yml/runs?head_sha=${sha}&event=push`, [
			{ id: 10, head_sha: sha, head_branch: "main", event: "push", status: "completed", conclusion: "success" },
		]],
		["actions/runs/10/jobs?filter=latest", manifest.ciJobs.map((name) => ({ name, status: "completed", conclusion: "success" }))],
		["actions/runs/10/artifacts", [{ name: `release-candidate-${sha}`, expired: false }]],
		["actions/workflows/release.yml/runs?event=workflow_dispatch", []],
	]);
	const api = {
		async json(path) {
			if (!routes.has(path)) throw new Error(`unexpected API path ${path}`);
			return structuredClone(routes.get(path));
		},
		async pages(path) { return this.json(path); },
	};
	return { directory, release, env, manifest, save, put, routes, api, download: async () => directory };
}
