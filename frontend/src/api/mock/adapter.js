/**
 * Axios adapter that answers requests from the in-memory mock database
 * instead of the network. Enabled with VITE_USE_MOCK=true.
 */
import { AxiosError } from 'axios';
import { findRoute } from './routes';
import { getDb, saveDb } from './db';
import { HttpError, MockResponse } from './errors';
import { TOKEN_PREFIX } from './handlers/auth';

const LATENCY_MS = 150;

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function toPath(config) {
	const base = config.baseURL ?? '';
	const url = config.url ?? '';
	return url.startsWith(base) ? url.slice(base.length) : url;
}

function parseBody(config) {
	if (typeof config.data !== 'string') return config.data ?? {};
	try {
		return JSON.parse(config.data);
	} catch {
		return {};
	}
}

function authenticate(db, config) {
	const header = config.headers?.get?.('Authorization') ?? config.headers?.Authorization ?? '';
	if (!header.startsWith(`Bearer ${TOKEN_PREFIX}`)) return null;
	const id = Number(header.slice(`Bearer ${TOKEN_PREFIX}`.length));
	const user = db.users.find((u) => u.id === id);
	return user && user.status === 'ACTIVE' ? user : null;
}

function execute(config) {
	const db = getDb();
	const method = (config.method ?? 'get').toUpperCase();
	const found = findRoute(method, toPath(config));
	if (!found) throw new HttpError(404, `No mock endpoint for ${method} ${toPath(config)}`);

	const { route, params } = found;
	const user = authenticate(db, config);
	if (!route.access.public) {
		if (!user) throw new HttpError(401, 'Please sign in again.');
		if (route.access.role && route.access.role !== user.role) throw new HttpError(403, 'You are not allowed to do that.');
	}

	const result = route.handler({ db, user, params, query: config.params ?? {}, body: parseBody(config) });
	if (method !== 'GET') saveDb();
	return result instanceof MockResponse ? result : new MockResponse(200, result);
}

export async function mockAdapter(config) {
	await delay(LATENCY_MS);

	let status;
	let data;
	try {
		const result = execute(config);
		status = result.status;
		// Clone so the UI can never mutate the mock database by accident.
		data = result.data === null ? null : JSON.parse(JSON.stringify(result.data));
	} catch (error) {
		if (!(error instanceof HttpError)) throw error;
		status = error.status;
		data = { message: error.message };
	}

	const response = { data, status, statusText: String(status), headers: {}, config, request: {} };
	if (status >= 400) {
		const code = status >= 500 ? AxiosError.ERR_BAD_RESPONSE : AxiosError.ERR_BAD_REQUEST;
		throw new AxiosError(data.message, code, config, {}, response);
	}
	return response;
}
