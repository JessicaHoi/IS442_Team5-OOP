import { badRequest, notFound } from '../errors';
import { route } from '../router';
import { toOwnProfile, toPublicProfile } from '../serializers';
import { validateSlots } from '../../../utils/availability';
import { MEETING_MODES, GROUP_FORMATS, STUDY_GOALS } from '../../../utils/constants';

const isOneOf = (options, value) => options.some((option) => option.value === value);

route('GET', '/students/me', ({ db, user }) => toOwnProfile(db, user.id), { role: 'STUDENT' });

route(
	'PUT',
	'/students/me/profile',
	({ db, user, body }) => {
		const { name, school, programme, yearOfStudy, contactNumber, courses } = body;
		if (!name?.trim() || !school || !programme?.trim() || !yearOfStudy || !contactNumber?.trim()) {
			throw badRequest('Name, school, programme, year of study and contact number are required.');
		}
		if (!Array.isArray(courses) || courses.length === 0) throw badRequest('Select at least one course.');
		if (courses.some((code) => !db.courses.some((course) => course.code === code))) {
			throw badRequest('Unknown course selected.');
		}

		const student = db.students[user.id];
		Object.assign(student, { school, programme: programme.trim(), yearOfStudy: Number(yearOfStudy), contactNumber: contactNumber.trim(), courses });
		db.users.find((u) => u.id === user.id).name = name.trim();
		if (student.preferences.course && !courses.includes(student.preferences.course)) {
			student.preferences.course = null;
		}
		return toOwnProfile(db, user.id);
	},
	{ role: 'STUDENT' },
);

route(
	'PUT',
	'/students/me/preferences',
	({ db, user, body }) => {
		const student = db.students[user.id];
		const { course, meetingMode, groupFormat, goals, availability } = body;
		if (!course || !student.courses.includes(course)) throw badRequest('Choose one of the courses you are taking.');
		if (!isOneOf(MEETING_MODES, meetingMode)) throw badRequest('Choose a meeting mode.');
		if (!isOneOf(GROUP_FORMATS, groupFormat)) throw badRequest('Choose a group format.');
		if (!Array.isArray(goals) || goals.length === 0 || goals.some((goal) => !isOneOf(STUDY_GOALS, goal))) {
			throw badRequest('Choose at least one study goal.');
		}
		if (!Array.isArray(availability) || availability.length === 0 || validateSlots(availability).some(Boolean)) {
			throw badRequest('Add at least one valid availability slot.');
		}
		student.preferences = { course, meetingMode, groupFormat, goals, availability };
		return toOwnProfile(db, user.id);
	},
	{ role: 'STUDENT' },
);

route(
	'GET',
	'/students/:id',
	({ db, user, params }) => {
		const id = Number(params.id);
		if (!db.students[id]) throw notFound('Student not found.');
		return toPublicProfile(db, id, user.id);
	},
	{ role: 'STUDENT' },
);
