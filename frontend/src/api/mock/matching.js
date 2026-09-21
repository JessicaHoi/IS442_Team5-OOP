/**
 * Stand-in matching engine for the mock API.
 * The real engine lives in the Spring Boot backend; this one follows the same
 * idea (Appendix A of the brief) so the UI can be demonstrated end to end.
 */
import { overlapMinutes } from '../../utils/availability';

/** Weekly overlap that earns a full availability score. */
export const FULL_AVAILABILITY_MINUTES = 180;

/** 1 when identical, 0.75 when either side is flexible, 0 when they conflict. */
function compatibility(a, b) {
	if (a === b) return 1;
	if (a === 'EITHER' || b === 'EITHER') return 0.75;
	return 0;
}

function jaccard(listA, listB) {
	const union = new Set([...listA, ...listB]);
	if (union.size === 0) return 0;
	const shared = listA.filter((item) => listB.includes(item)).length;
	return shared / union.size;
}

function courseScore(me, other, targetCourse) {
	if (targetCourse) {
		if (!other.courses.includes(targetCourse)) return other.courses.some((c) => me.courses.includes(c)) ? 0.25 : 0;
		return other.preferences.course === targetCourse ? 1 : 0.75;
	}
	return other.courses.some((c) => me.courses.includes(c)) ? 0.5 : 0;
}

/** Score each criterion between 0 and 1. */
export function scoreCriteria(me, other, targetCourse) {
	const overlap = overlapMinutes(me.preferences.availability, other.preferences.availability);
	return {
		course: courseScore(me, other, targetCourse ?? me.preferences.course),
		availability: Math.min(1, overlap / FULL_AVAILABILITY_MINUTES),
		studyMode: compatibility(me.preferences.meetingMode, other.preferences.meetingMode),
		studyGoal: jaccard(me.preferences.goals, other.preferences.goals),
		groupSize: compatibility(me.preferences.groupFormat, other.preferences.groupFormat),
		overlapMinutes: overlap,
	};
}

/** Weighted average of the enabled criteria, as a whole number from 0 to 100. */
export function combineScore(config, criteria) {
	const enabled = config.criteria.filter((item) => item.enabled && item.weight > 0);
	const totalWeight = enabled.reduce((sum, item) => sum + item.weight, 0);
	if (totalWeight === 0) return 0;
	const weighted = enabled.reduce((sum, item) => sum + item.weight * criteria[item.key], 0);
	return Math.round((weighted / totalWeight) * 100);
}

/** Criterion a strategy ranks by before looking at the overall score. */
const STRATEGY_PRIMARY_CRITERION = {
	BALANCED: null,
	AVAILABILITY_FIRST: 'availability',
	COURSE_FIRST: 'course',
};

/**
 * Rank candidates for `me`. Returns items sorted best first.
 * Strategies that prioritise a criterion group candidates into quarter-point
 * tiers of that criterion, then sort by total score inside each tier.
 */
export function rankCandidates(me, candidates, config, targetCourse) {
	const primary = STRATEGY_PRIMARY_CRITERION[config.strategy];
	const tier = (criteria) => (primary ? Math.round(criteria[primary] * 4) / 4 : 0);

	return candidates
		.map((candidate) => {
			const { overlapMinutes: overlap, ...breakdown } = scoreCriteria(me, candidate.profile, targetCourse);
			return {
				...candidate,
				breakdown,
				overlapMinutes: overlap,
				score: combineScore(config, breakdown),
				tier: tier(breakdown),
			};
		})
		.sort((a, b) => b.tier - a.tier || b.score - a.score || a.name.localeCompare(b.name));
}
