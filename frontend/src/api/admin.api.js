import { http, unwrap } from './http';

export const adminApi = {
	listUsers: () => unwrap(http.get('/admin/users')),
	createUser: (user) => unwrap(http.post('/admin/users', user)),
	updateUser: (id, user) => unwrap(http.put(`/admin/users/${id}`, user)),
	deleteUser: (id) => unwrap(http.delete(`/admin/users/${id}`)),

	getMatchingConfig: () => unwrap(http.get('/admin/matching-config')),
	updateMatchingConfig: (config) => unwrap(http.put('/admin/matching-config', config)),

	/** Students that can be used as the sample in the strategy preview. */
	listPreviewStudents: () => unwrap(http.get('/admin/matching-config/preview/students')),
	/** Rank one student under the saved settings and under `proposed` (nothing is saved). */
	previewMatching: (studentId, course, proposed) =>
		unwrap(http.post('/admin/matching-config/preview', { studentId, course, proposed })),
};
