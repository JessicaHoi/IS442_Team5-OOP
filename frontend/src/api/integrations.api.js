import { http, unwrap } from './http';

export const integrationsApi = {
	/** Which optional integrations this server has on. */
	get: () => unwrap(http.get('/integrations')),
};
