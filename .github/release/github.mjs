import assert from "node:assert/strict";

/** Bounded, read-only API access; an unavailable service must never be mistaken for an absent artifact. */
export async function get(url, authorization, signal = AbortSignal.timeout(30_000)) {
	const response = await fetch(url, {
		headers: authorization ? { Authorization: authorization } : {},
		signal,
		redirect: "error",
	});
	if (response.status === 404) return null;
	assert.ok(response.ok, `GET ${new URL(url).pathname}: HTTP ${response.status}`);
	return Buffer.from(await response.arrayBuffer());
}

export function github(repository, token) {
	const base = `https://api.github.com/repos/${repository}/`;
	return {
		async json(path) {
			const bytes = await get(base + path, `Bearer ${token}`);
			assert.ok(bytes, `missing GitHub API resource: ${path}`);
			return JSON.parse(bytes.toString("utf8"));
		},
		async pages(path, key) {
			const items = [];
			for (let page = 1; ; page++) {
				const bytes = await get(`${base}${path}${path.includes("?") ? "&" : "?"}per_page=100&page=${page}`, `Bearer ${token}`);
				assert.ok(bytes, `missing GitHub API resource: ${path}`);
				const result = JSON.parse(bytes.toString("utf8"));
				const batch = key ? result[key] : result;
				assert.ok(Array.isArray(batch), "API list required");
				items.push(...batch);
				if (batch.length < 100) return items;
			}
		},
	};
}
