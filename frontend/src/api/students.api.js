import { http, unwrap } from './http';

export const studentsApi = {
	getMe: () => unwrap(http.get('/students/me')),
	updateProfile: (profile) => unwrap(http.put('/students/me/profile', profile)),
	/** Replaces all study preferences (one per course). */
	updatePreferences: (preferences) => unwrap(http.put('/students/me/preferences', { preferences })),
	getPublicProfile: (id) => unwrap(http.get(`/students/${id}`)),
};
