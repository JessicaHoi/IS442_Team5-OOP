import { http, unwrap } from './http';

export const coursesApi = {
	list: () => unwrap(http.get('/public/courses')),
	create: (courseCode, courseName) => unwrap(http.post('/courses', { courseCode, courseName })),
};
