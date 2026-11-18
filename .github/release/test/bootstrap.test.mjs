import assert from "node:assert/strict";
import { once } from "node:events";
import { copyFileSync, mkdirSync, readFileSync, symlinkSync, writeFileSync } from "node:fs";
import { createServer } from "node:http";
import { join } from "node:path";
import { test } from "node:test";
import { preflight } from "../bootstrap.mjs";
import { artifactNames, identity, verifyCandidate } from "../candidate.mjs";
import { get } from "../github.mjs";
import { needsPublication, recoveryPlan, verifySecrets, verifyStaged } from "../recovery.mjs";
import { fixture } from "./bootstrap-fixture.mjs";

test("approved source resolves an annotated tag and validates every gate", async (t) => {
	const f = fixture(t);
	const result = await preflight(f.env, f.api, f.download);
	assert.equal(result.sha, f.release.sha);
	assert.equal(result.ciRun, 10);
	assert.equal(result.tagObject, "b".repeat(40));
});

for (const [name, values, reason] of [
	["bad tag", { RELEASE_TAG: "v2.0.0-dev" }, /stable tag/],
	["shell injection", { RELEASE_TAG: "v2.0.0; touch /tmp/should-never-exist" }, /stable tag/],
	["tag newline", { RELEASE_TAG: "v2.0.0\nx=y" }, /stable tag/],
	["trailing tag newline", { RELEASE_TAG: "v2.0.0\n" }, /stable tag/],
	["leading-zero version", { RELEASE_TAG: "v02.0.0" }, /stable tag/],
	["abbreviated SHA", { RELEASE_SHA: "abcd" }, /full immutable/],
	["trailing SHA newline", { RELEASE_SHA: `${"a".repeat(40)}\n` }, /full immutable/],
	["SHA shell injection", { RELEASE_SHA: `$(echo ${"a".repeat(40)})` }, /full immutable/],
	["fork or branch dispatch", { GITHUB_REF: "refs/heads/feature" }, /must use main/],
	["automatic dispatch", { GITHUB_EVENT_NAME: "push" }, /manual dispatch/],
	["premature remotePublicationApproved", { ORG_GRADLE_PROJECT_remotePublicationApproved: "true" }, /premature/],
]) {
	test(`${name} stops before any API call or external write`, async (t) => {
		const f = fixture(t);
		await assert.rejects(preflight({ ...f.env, ...values }, {}, f.download), reason);
	});
}

for (const [name, change, reason] of [
	["SHA not on main", (f) => { f.routes.set(`compare/${f.release.sha}...main`, { merge_base_commit: { sha: "c".repeat(40) } }); }, /not on main/],
	["tag targets another SHA", (f) => { f.routes.set(`git/tags/${"b".repeat(40)}`, { object: { type: "commit", sha: "c".repeat(40) } }); }, /tag SHA mismatch/],
	["incomplete CI", (f) => { f.routes.get("actions/runs/10/jobs?filter=latest")[0].status = "in_progress"; }, /incomplete CI jobs/],
	["skipped CI", (f) => { f.routes.get("actions/runs/10/jobs?filter=latest")[0].conclusion = "skipped"; }, /incomplete CI jobs/],
	["zero CI jobs", (f) => { f.routes.set("actions/runs/10/jobs?filter=latest", []); }, /incomplete CI jobs/],
	["stale CI success", (f) => {
		f.routes.get(`actions/workflows/ci.yml/runs?head_sha=${f.release.sha}&event=push`).push({
			id: 11, head_sha: f.release.sha, head_branch: "main", event: "push", status: "completed", conclusion: "failure",
		});
	}, /complete successful CI/],
	["wrong SHA CI", (f) => { f.routes.get(`actions/workflows/ci.yml/runs?head_sha=${f.release.sha}&event=push`)[0].head_sha = "c".repeat(40); }, /complete successful CI/],
	["missing candidate", (f) => { f.routes.set("actions/runs/10/artifacts", []); }, /one unexpired/],
	["expired evidence", (f) => { f.routes.get("actions/runs/10/artifacts")[0].expired = true; }, /one unexpired/],
	["version mismatch", (f) => { f.manifest.version = "2.0.1"; f.save(); }, /candidate version mismatch/],
	["missing runtime row", (f) => { delete f.manifest.gates.testkit8; f.save(); }, /missing testkit8 evidence/],
	["missing CI inventory row", (f) => { f.manifest.ciJobs.pop(); f.save(); }, /CI job inventory mismatch/],
	["missing source archive", (f) => { delete f.manifest.artifacts["atrium-gradle-testkit-2.0.0-sources.jar"]; f.save(); }, /exact neutral publication/],
	["zero tests with success status", (f) => { f.put("gates/host.json", JSON.stringify({ sha: f.release.sha, status: "passed", tests: 0, failures: 0, skipped: 0 })); f.save(); }, /host incomplete tests/],
	["stale hash", (f) => { f.put("gates/host.json", "modified"); }, /candidate hash mismatch/],
	["unreviewed notes", (f) => { f.manifest.notesReviewed = false; f.save(); }, /reviewed release notes/],
	["changed tag after approval", (f) => { f.env.EXPECTED_TAG_OBJECT = "c".repeat(40); }, /tag was moved/],
	["changed candidate after approval", (f) => { f.env.EXPECTED_MANIFEST_HASH = "0".repeat(64); }, /candidate evidence changed/],
	["CI replaced after approval", (f) => { f.env.EXPECTED_CI_RUN = "9"; }, /CI run changed/],
	["environment without required owner review", (f) => { f.routes.get("environments/release-bootstrap").protection_rules = []; }, /owner review/],
	["environment permits another reviewer instead of the owner", (f) => {
		f.routes.get("environments/release-bootstrap").protection_rules[0].reviewers.push({ type: "User", reviewer: { login: "another-reviewer" } });
	}, /owner review/],
	["missing environment", (f) => { f.routes.delete("environments/release-bootstrap"); }, /unexpected API path environments/],
	["environment permits untrusted branches", (f) => { f.routes.get("environments/release-bootstrap/deployment-branch-policies").push({ name: "*", type: "branch" }); }, /main only/],
]) {
	test(`${name} stops before candidate publishing`, async (t) => {
		const f = fixture(t);
		change(f);
		await assert.rejects(preflight(f.env, f.api, f.download), reason);
	});
}

test("candidate traversal and symlinks cannot import unreviewed external files", (t) => {
	const f = fixture(t);
	symlinkSync("/etc/hosts", join(f.directory, "outside"));
	f.manifest.files.outside = "0".repeat(64);
	f.save();
	assert.throws(() => verifyCandidate(f.directory, f.release));
});

test("missing secrets block before any destination write", () => {
	const secrets = {
		MAVEN_CENTRAL_USERNAME: "fixture", MAVEN_CENTRAL_PASSWORD: "fixture",
		PACKAGE_SIGNING_KEY: "fixture", PACKAGE_SIGNING_KEY_PASSWORD: "fixture", GH_TOKEN: "fixture",
	};
	verifySecrets(secrets);
	for (const name of Object.keys(secrets)) {
		assert.throws(() => verifySecrets({ ...secrets, [name]: "" }), new RegExp(name));
	}
});

test("partial destination recovery skips immutable Central and resumes Packages at the same version", async (t) => {
	const f = fixture(t);
	const candidate = await preflight(f.env, f.api, f.download);
	const readCentral = async (name) => name.endsWith(".asc") ? Buffer.from("signature") : readFileSync(join(f.directory, name));
	const plan = await recoveryPlan(candidate, { api: f.api, central: readCentral, packages: async () => null });
	assert.deepEqual(plan, { central: false, packages: true });
	assert.equal(candidate.version, "2.0.0");
});

test("uncertain cancelled Portal upload cannot be uploaded a second time", async (t) => {
	const f = fixture(t);
	const candidate = await preflight(f.env, f.api, f.download);
	f.routes.set("actions/workflows/release.yml/runs?event=workflow_dispatch", [{ id: 1, display_title: `Bootstrap ${f.release.tag} at ${f.release.sha}` }]);
	f.routes.set("actions/runs/1/jobs?filter=all", [{
		steps: [{ name: "Publish Maven artifacts", status: "completed", conclusion: "cancelled" }],
	}]);
	await assert.rejects(recoveryPlan(candidate, {
		api: f.api, central: async () => null, packages: async () => null,
	}), /uncertain state/);
});

test("a tag moved during rebuild blocks before destination access", async (t) => {
	const f = fixture(t);
	const candidate = await preflight(f.env, f.api, f.download);
	f.routes.set(`git/ref/tags/${f.release.tag}`, { object: { type: "commit", sha: f.release.sha } });
	await assert.rejects(recoveryPlan(candidate, { api: f.api }), /tag was moved during rebuild/);
});

test("a new CI run during rebuild blocks before destination access", async (t) => {
	const f = fixture(t);
	const candidate = await preflight(f.env, f.api, f.download);
	f.routes.get(`actions/workflows/ci.yml/runs?head_sha=${f.release.sha}&event=push`)[0].id = 11;
	f.routes.set("actions/runs/11/jobs?filter=latest", f.routes.get("actions/runs/10/jobs?filter=latest"));
	await assert.rejects(recoveryPlan(candidate, { api: f.api }), /CI run changed during rebuild/);
});

for (const kind of ["partial", "mismatch", "unsigned"]) {
	test(`${kind} destination never overwrites Central`, async (t) => {
		const f = fixture(t);
		const first = artifactNames(f.release.version)[0];
		await assert.rejects(needsPublication(verifyCandidate(f.directory, f.release), async (name) => {
			if (kind === "mismatch") return Buffer.from("wrong version bytes");
			if (name.endsWith(".asc")) return kind === "unsigned" ? null : Buffer.from("signature");
			return kind === "partial" && name !== first ? null : readFileSync(join(f.directory, name));
		}));
	});
}

test("rebuilt artifacts must match reviewed candidate bytes", (t) => {
	const f = fixture(t);
	const candidate = verifyCandidate(f.directory, f.release);
	const repository = join(f.directory, "staging");
	const staged = join(repository, "de/joshuagleitze/atrium-gradle-testkit", f.release.version);
	mkdirSync(staged, { recursive: true });
	for (const name of Object.keys(candidate.manifest.artifacts)) copyFileSync(join(f.directory, name), join(staged, name));
	verifyStaged(candidate, repository);

	const altered = artifactNames(f.release.version)[0];
	writeFileSync(join(staged, altered), "rebuilt with different bytes");
	assert.throws(() => verifyStaged(candidate, repository), new RegExp(`rebuilt artifact differs from candidate: ${altered}`));
});

test("source identity never authorizes a production publication", (t) => {
	const f = fixture(t);
	assert.deepEqual(identity(f.env), f.release);
	assert.equal(f.env.ORG_GRADLE_PROJECT_remotePublicationApproved, undefined);
});

test("wire-level GET distinguishes absence, service errors and hung commands", { timeout: 5000 }, async (t) => {
	const server = createServer((request, response) => {
		if (request.url === "/hang") return;
		response.writeHead(request.url === "/missing" ? 404 : 503);
		response.end("BUILD SUCCESSFUL");
	});
	t.after(() => { server.closeAllConnections(); server.close(); });
	server.listen(0, "127.0.0.1");
	await once(server, "listening");
	const base = `http://127.0.0.1:${server.address().port}`;
	assert.equal(await get(`${base}/missing`), null);
	await assert.rejects(get(`${base}/failure`), /HTTP 503/);
	const abort = new AbortController();
	const requested = once(server, "request");
	const hung = get(`${base}/hang`, undefined, abort.signal);
	await requested;
	abort.abort();
	await assert.rejects(hung, { name: "AbortError" });
});

test("registry reads preserve HTTP Basic authorization and artifact bytes", async (t) => {
	const authorization = `Basic ${Buffer.from("owner:fixture-token").toString("base64")}`;
	const server = createServer((request, response) => {
		response.writeHead(request.headers.authorization === authorization ? 200 : 401);
		response.end(Buffer.from([0, 1, 254, 255]));
	});
	t.after(() => { server.closeAllConnections(); server.close(); });
	server.listen(0, "127.0.0.1");
	await once(server, "listening");
	assert.deepEqual(await get(`http://127.0.0.1:${server.address().port}/artifact.jar`, authorization), Buffer.from([0, 1, 254, 255]));
});
