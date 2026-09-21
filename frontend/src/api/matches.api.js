import { http, unwrap } from './http';

/** Remove empty filter values so they are not sent as query parameters. */
const compact = (params) => Object.fromEntries(Object.entries(params).filter(([, value]) => value));

export const matchesApi = {
	/** @param {{course?: string, goal?: string, day?: string, studyMode?: string, groupFormat?: string}} filters */
	search: (filters) => unwrap(http.get('/matches', { params: compact(filters) })),
};
