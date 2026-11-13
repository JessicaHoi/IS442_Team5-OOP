import { beforeEach, describe, expect, it } from 'vitest';
import { DEFAULT_MATCHING_CONFIG, MATCHING_STRATEGIES } from '../../utils/constants';
import { mockAdapter } from './adapter';
import { getDb, resetDb } from './db';
import { issueSession } from './sessions';

const invalidConfigs = [
	['missing config', () => null],
	['missing strategy', (config) => { delete config.strategy; }],
	['unknown strategy', (config) => { config.strategy = 'UNKNOWN'; }],
	['missing criteria', (config) => { delete config.criteria; }],
	['non-array criteria', (config) => { config.criteria = {}; }],
	['missing criterion', (config) => { config.criteria.pop(); }],
	['unknown criterion', (config) => { config.criteria.push({ key: 'unknown', weight: 1, enabled: true }); }],
	['duplicate criterion', (config) => { config.criteria.push({ ...config.criteria[0] }); }],
	['null criterion', (config) => { config.criteria[0] = null; }],
	['missing enabled flag', (config) => { delete config.criteria[0].enabled; }],
	['invalid enabled flag', (config) => { config.criteria[0].enabled = 'false'; }],
	['null enabled flag', (config) => { config.criteria[0].enabled = null; }],
	['numeric enabled flag', (config) => { config.criteria[0].enabled = 1; }],
	['missing weight', (config) => { delete config.criteria[0].weight; }],
	['fractional weight', (config) => { config.criteria[0].weight = 1.5; }],
	['negative weight', (config) => { config.criteria[0].weight = -1; }],
	['excessive disabled weight', (config) => { config.criteria[0] = { key: 'course', enabled: false, weight: 101 }; }],
	['string weight', (config) => { config.criteria[0].weight = '30'; }],
	['null weight', (config) => { config.criteria[0].weight = null; }],
	['boolean weight', (config) => { config.criteria[0].weight = true; }],
	['zero enabled weight', (config) => { config.criteria.forEach((criterion) => { criterion.weight = 0; }); }],
	['all disabled', (config) => { config.criteria.forEach((criterion) => { criterion.enabled = false; }); }],
];

describe.each(['save', 'preview'])('mock matching configuration %s', (operation) => {
	let token;
	beforeEach(() => {
		resetDb();
		const db = getDb();
		token = issueSession(db, db.users.find((user) => user.role === 'ADMIN'));
	});
	const send = (config, token) => mockAdapter({
		method: operation === 'save' ? 'put' : 'post',
		url: `/admin/matching-config${operation === 'preview' ? '/preview' : ''}`,
		headers: { Authorization: `Bearer ${token}` },
		data: operation === 'preview' ? { studentId: 2, course: 'IS442', proposed: config } : config,
	});

	it.each(invalidConfigs)('rejects %s without changing saved settings', async (_, change) => {
		const config = structuredClone(DEFAULT_MATCHING_CONFIG);
		const changed = change(config);
		await expect(send(changed === null ? null : config, token)).rejects.toMatchObject({ response: { status: 400 } });
		expect(getDb().matchingConfig).toEqual(DEFAULT_MATCHING_CONFIG);
	});

	it.each(MATCHING_STRATEGIES.map(({ value }) => value))('accepts %s and boundary weights in any criterion order', async (strategy) => {
		const config = structuredClone(DEFAULT_MATCHING_CONFIG);
		config.strategy = strategy;
		config.criteria.forEach((criterion, i) => { criterion.weight = i === 0 ? 100 : 0; });
		config.criteria.reverse();
		expect((await send(config, token)).status).toBe(200);
		if (operation === 'save') expect(getDb().matchingConfig.criteria[0]).toEqual({ key: 'course', weight: 100, enabled: true });
		else expect(getDb().matchingConfig).toEqual(DEFAULT_MATCHING_CONFIG);
	});

	it.each(['0', '100', '30.0', '3e1'])('accepts whole-number JSON weight %s', async (literal) => {
		const config = structuredClone(DEFAULT_MATCHING_CONFIG);
		const body = operation === 'preview' ? { studentId: 2, course: 'IS442', proposed: config } : config;
		const response = await mockAdapter({
			method: operation === 'save' ? 'put' : 'post',
			url: `/admin/matching-config${operation === 'preview' ? '/preview' : ''}`,
			headers: { Authorization: `Bearer ${token}` },
			data: JSON.stringify(body).replace('"weight":30', `"weight":${literal}`),
		});
		expect(response.status).toBe(200);
	});
});
