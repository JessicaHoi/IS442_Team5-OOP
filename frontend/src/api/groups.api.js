import { http, unwrap } from './http';

export const groupsApi = {
	list: (course) => unwrap(http.get('/groups', { params: course ? { course } : {} })),
	mine: () => unwrap(http.get('/groups/mine')),
	get: (id) => unwrap(http.get(`/groups/${id}`)),
	create: (group) => unwrap(http.post('/groups', group)),
	update: (id, group) => unwrap(http.put(`/groups/${id}`, group)),
	close: (id) => unwrap(http.post(`/groups/${id}/close`)),

	requestToJoin: (id, message) => unwrap(http.post(`/groups/${id}/join-requests`, { message })),
	joinRequests: (id) => unwrap(http.get(`/groups/${id}/join-requests`)),
	acceptRequest: (id, requestId) => unwrap(http.post(`/groups/${id}/join-requests/${requestId}/accept`)),
	rejectRequest: (id, requestId) => unwrap(http.post(`/groups/${id}/join-requests/${requestId}/reject`)),
	removeMember: (id, studentId) => unwrap(http.delete(`/groups/${id}/members/${studentId}`)),
};
