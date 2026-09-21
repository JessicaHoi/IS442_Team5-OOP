/** Tiny path router for the mock API: "/groups/:id/close" -> handler. */

const routes = [];

function compile(pattern) {
	const names = [];
	const source = pattern.replace(/:([A-Za-z]+)/g, (_, name) => {
		names.push(name);
		return '([^/]+)';
	});
	return { regex: new RegExp(`^${source}$`), names };
}

/**
 * Register a handler.
 * @param {string} method HTTP method.
 * @param {string} pattern Path such as "/students/:id".
 * @param {(ctx: object) => any} handler Receives { db, user, params, query, body }.
 * @param {{ public?: boolean, role?: string }} [access] Who may call it. Default: any signed-in user.
 */
export function route(method, pattern, handler, access = {}) {
	routes.push({ method, ...compile(pattern), handler, access });
}

export function findRoute(method, path) {
	for (const candidate of routes) {
		if (candidate.method !== method) continue;
		const match = candidate.regex.exec(path);
		if (!match) continue;
		const params = {};
		candidate.names.forEach((name, i) => {
			params[name] = decodeURIComponent(match[i + 1]);
		});
		return { route: candidate, params };
	}
	return null;
}
