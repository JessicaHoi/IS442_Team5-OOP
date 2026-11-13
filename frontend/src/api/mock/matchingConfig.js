import { MATCHING_CRITERIA, MATCHING_STRATEGIES } from '../../utils/constants';
import { badRequest } from './errors';

/** Shared contract for saved settings and unsaved strategy previews. */
export function validateMatchingConfig(config) {
	if (!MATCHING_STRATEGIES.some((strategy) => strategy.value === config?.strategy)) throw badRequest('Choose a matching strategy.');
	if (!Array.isArray(config.criteria)) throw badRequest('Every matching criterion must be configured.');
	const settings = new Map();
	for (const criterion of config.criteria) {
		if (!criterion || !MATCHING_CRITERIA.some(({ key }) => key === criterion.key)) throw badRequest('Unknown matching criterion.');
		if (settings.has(criterion.key)) throw badRequest('Each matching criterion can only be configured once.');
		if (typeof criterion.enabled !== 'boolean') throw badRequest('Choose whether each criterion is enabled.');
		if (!Number.isInteger(criterion.weight) || criterion.weight < 0 || criterion.weight > 100) {
			throw badRequest('Weights must be whole numbers from 0 to 100.');
		}
		settings.set(criterion.key, criterion);
	}
	if (settings.size !== MATCHING_CRITERIA.length) throw badRequest('Every matching criterion must be configured.');
	const criteria = MATCHING_CRITERIA.map(({ key }) => {
		const { enabled, weight } = settings.get(key);
		return { key, enabled, weight };
	});
	if (!criteria.some((criterion) => criterion.enabled && criterion.weight > 0)) throw badRequest('Enable at least one criterion with a weight above 0.');
	return { strategy: config.strategy, criteria };
}
