/**
 * The only module that knows about Axios. Every resource module in this
 * folder goes through this client, so auth, errors and the mock switch
 * are handled in one place.
 */
import axios from 'axios';

/** Error thrown for every failed request, with a message that is safe to show. */
export class ApiError extends Error {
	constructor(message, status) {
		super(message);
		this.name = 'ApiError';
		this.status = status;
	}
}

const handlers = {
	getToken: () => null,
	onUnauthorized: () => {},
};

/** Wire session behaviour in from outside (keeps this module free of store imports). */
export function configureHttp({ getToken, onUnauthorized }) {
	handlers.getToken = getToken;
	handlers.onUnauthorized = onUnauthorized;
}

export const http = axios.create({
	baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
	timeout: 15000,
	headers: { 'Content-Type': 'application/json' },
});

// The flag is read inline so the bundler can drop the mock code from production builds.
if (import.meta.env.VITE_USE_MOCK === 'true') {
	// Loaded lazily; never part of a production bundle.
	http.defaults.adapter = (config) => import('./mock/adapter').then((module) => module.mockAdapter(config));
}

http.interceptors.request.use((config) => {
	const token = handlers.getToken();
	if (token) config.headers.Authorization = `Bearer ${token}`;
	return config;
});

http.interceptors.response.use(
	(response) => response,
	(error) => {
		const status = error.response?.status;
		const data = error.response?.data;
		const message =
			data?.message ||
			data?.error ||
			(error.request && !error.response ? 'Cannot reach the server. Please try again.' : error.message);

		const isLogin = error.config?.url?.endsWith('/auth/login');
		if (status === 401 && !isLogin) handlers.onUnauthorized();
		return Promise.reject(new ApiError(message, status));
	},
);

/** Unwrap `response.data` so resource modules stay short. */
export const unwrap = (request) => request.then((response) => response.data);
