import { route } from '../router';
import { rankCandidates } from '../matching';
import { connectionStatusBetween, toPublicProfile } from '../serializers';

const isCompatibleWith = (candidateValue, wanted) => !wanted || wanted === 'EITHER' || candidateValue === wanted || candidateValue === 'EITHER';

route(
	'GET',
	'/matches',
	({ db, user, query }) => {
		const me = db.students[user.id];
		const { course, goal, day, studyMode, groupFormat } = query;

		const candidates = db.users
			.filter((u) => u.role === 'STUDENT' && u.status === 'ACTIVE' && u.id !== user.id)
			.map((u) => ({ id: u.id, name: u.name, profile: db.students[u.id] }))
			.filter(({ profile }) => {
				const prefs = profile.preferences;
				return (
					(!course || profile.courses.includes(course)) &&
					(!goal || prefs.goals.includes(goal)) &&
					(!day || prefs.availability.some((slot) => slot.day === day)) &&
					isCompatibleWith(prefs.meetingMode, studyMode) &&
					isCompatibleWith(prefs.groupFormat, groupFormat)
				);
			});

		return rankCandidates(me, candidates, db.matchingConfig, course).map((ranked) => ({
			student: toPublicProfile(db, ranked.id, user.id),
			score: ranked.score,
			breakdown: ranked.breakdown,
			overlapMinutes: ranked.overlapMinutes,
			sharedCourses: ranked.profile.courses.filter((code) => me.courses.includes(code)),
			connectionStatus: connectionStatusBetween(db, user.id, ranked.id).status,
		}));
	},
	{ role: 'STUDENT' },
);
