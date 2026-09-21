import { http, unwrap } from './http';

export const coursesApi = {
	list: () => unwrap(http.get('/courses')),
};
