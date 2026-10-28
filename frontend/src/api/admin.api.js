import { http, unwrap } from './http';

export const adminApi = {
	listUsers: () => unwrap(http.get('/admin/users')),
	createUser: (user) => unwrap(http.post('/admin/users', user)),
	updateUser: (id, user) => unwrap(http.put(`/admin/users/${id}`, user)),
	deleteUser: (id) => unwrap(http.delete(`/admin/users/${id}`)),
	listLedGroups: (id) => unwrap(http.get(`/admin/users/${id}/led-groups`)),
	transferLeadership: (id, groupId, studentId) => unwrap(http.put(`/admin/users/${id}/led-groups/${groupId}/leader`, { studentId })),

	getMatchingConfig: () => unwrap(http.get('/admin/matching-config')),
	updateMatchingConfig: (config) => unwrap(http.put('/admin/matching-config', config)),
};
