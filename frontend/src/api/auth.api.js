import { http, unwrap } from './http';

export const authApi = {
	login: (email, password) => unwrap(http.post('/auth/login', { email, password })),
	me: () => unwrap(http.get('/auth/me')),
	/** Revokes `token` on the server. It is passed in because the store forgets it before this call. */
	logout: (token) => unwrap(http.post('/auth/logout', null, { headers: { Authorization: `Bearer ${token}` } })),
};
