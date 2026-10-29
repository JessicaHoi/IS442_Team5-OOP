/**
 * Mock version of the admin strategy preview. It follows the same rules as
 * the Spring Boot service so the UI can be shown without the backend.
 */
import { badRequest, notFound } from '../errors';
import { route } from '../router';
import { rankCandidates } from '../matching';
import { emptyPreference, preferenceFor } from '../../../utils/preferences';
import { MATCHING_CRITERIA } from '../../../utils/constants';

const ADMIN_ONLY = { role: 'ADMIN' };
const PREVIEW_SIZE = 10;
const SUPPORTED_STRATEGIES = ['BALANCED', 'AVAILABILITY_FIRST'];

const withPreferenceFor = (profile, course) => ({
	...profile,
	preferences: preferenceFor(profile.preferences, course) ?? emptyPreference(null),
});

const activeStudents = (db) => db.users.filter((u) => u.role === 'STUDENT' && u.status === 'ACTIVE');

function validateConfig(config) {
	if (!SUPPORTED_STRATEGIES.includes(config?.strategy)) throw badRequest('This matching strategy is not available yet.');
	const criteria = MATCHING_CRITERIA.map(({ key }) => config.criteria?.find((c) => c.key === key));
	if (criteria.some((c) => !c)) throw badRequest('Every matching criterion must be configured.');
	if (!criteria.some((c) => c.enabled && c.weight > 0)) throw badRequest('Enable at least one criterion with a weight above 0.');
	return { strategy: config.strategy, criteria };
}

function ranking(db, studentId, course, config) {
	const me = withPreferenceFor(db.students[studentId], course);
	const candidates = activeStudents(db)
		.filter((u) => u.id !== studentId && (!course || db.students[u.id].courses.includes(course)))
		.map((u) => ({ id: u.id, name: u.name, profile: withPreferenceFor(db.students[u.id], course) }));
	return rankCandidates(me, candidates, config, course);
}

function topEntries(ranked, otherRanked) {
	const otherRank = new Map(otherRanked.map((item, index) => [item.id, index + 1]));
	return ranked.slice(0, PREVIEW_SIZE).map((item, index) => ({
		rank: index + 1,
		otherRank: otherRank.get(item.id) ?? null,
		studentId: item.id,
		name: item.name,
		score: item.score,
		overlapMinutes: item.overlapMinutes,
		breakdown: item.breakdown,
	}));
}

route(
	'GET',
	'/admin/matching-config/preview/students',
	({ db }) =>
		activeStudents(db)
			.filter((u) => db.students[u.id]?.preferences?.length)
			.map((u) => ({ id: u.id, name: u.name, courses: db.students[u.id].preferences.map((p) => p.course) }))
			.sort((a, b) => a.name.localeCompare(b.name)),
	ADMIN_ONLY,
);

route(
	'POST',
	'/admin/matching-config/preview',
	({ db, body }) => {
		const proposed = validateConfig(body.proposed);
		const user = db.users.find((u) => u.id === Number(body.studentId) && u.role === 'STUDENT');
		if (!user) throw notFound('Student not found.');
		const course = body.course || null;
		const current = ranking(db, user.id, course, db.matchingConfig);
		const next = ranking(db, user.id, course, proposed);
		return {
			studentName: user.name,
			course,
			current: { strategy: db.matchingConfig.strategy, results: topEntries(current, next) },
			proposed: { strategy: proposed.strategy, results: topEntries(next, current) },
		};
	},
	ADMIN_ONLY,
);
