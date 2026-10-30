import { http, unwrap } from './http';

export const sessionsApi = {
	upcoming: () => unwrap(http.get('/sessions/upcoming')),
	list: (groupId, scope) => unwrap(http.get(`/groups/${groupId}/sessions`, { params: { scope } })),
	create: (groupId, session) => unwrap(http.post(`/groups/${groupId}/sessions`, session)),
	update: (groupId, id, session) => unwrap(http.put(`/groups/${groupId}/sessions/${id}`, session)),
	cancel: (groupId, id) => unwrap(http.post(`/groups/${groupId}/sessions/${id}/cancel`)),
	respond: (groupId, id, response) => unwrap(http.put(`/groups/${groupId}/sessions/${id}/rsvp`, { response })),
	/** Goes through Axios, not a plain link, because only the Bearer header signs the request in. */
	calendarFile: (groupId, id) => unwrap(http.get(`/groups/${groupId}/sessions/${id}/calendar.ics`, { responseType: 'text' })),
};
