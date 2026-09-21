import { http, unwrap } from './http';

export const connectionsApi = {
	send: (toStudentId, message) => unwrap(http.post('/connections', { toStudentId, message })),
	/** @param {'incoming'|'sent'|'active'} view */
	list: (view) => unwrap(http.get('/connections', { params: { view } })),
	accept: (id) => unwrap(http.post(`/connections/${id}/accept`)),
	decline: (id) => unwrap(http.post(`/connections/${id}/decline`)),
	end: (id) => unwrap(http.delete(`/connections/${id}`)),
};
