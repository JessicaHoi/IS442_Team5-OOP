import { badRequest, conflict, created, HttpError, noContent } from '../errors';
import { route } from '../router';
import { nextId, saveDb } from '../db';
import { issueSession, revokeSession } from '../sessions';
import { INVALID_HANDLE_MESSAGE, normalizeTelegramHandle } from '../../../utils/telegram';
import { courseInputError, passwordError, PHONE_PATTERN } from '../../../utils/validation';

const toSessionUser = (user) => ({ id: user.id, name: user.name, email: user.email, role: user.role });
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function normalizeCourse(course) {
	const courseCode = typeof course?.courseCode === 'string' ? course.courseCode.trim().toUpperCase() : '';
	const courseName = typeof course?.courseName === 'string' ? course.courseName.trim() : '';
	const error = courseInputError(courseCode, courseName);
	if (error) throw badRequest(error);
	return { code: courseCode, name: courseName, school: null };
}

route(
	'POST',
	'/auth/register',
	({ db, body }) => {
		const name = typeof body.name === 'string' ? body.name.trim() : '';
		const email = typeof body.email === 'string' ? body.email.trim() : '';
		const password = typeof body.password === 'string' ? body.password : '';
		const school = typeof body.school === 'string' ? body.school.trim() : '';
		const programme = typeof body.programme === 'string' ? body.programme.trim() : '';
		const contactNumber = typeof body.contactNumber === 'string' ? body.contactNumber.trim() : '';
		const newCourses = Array.isArray(body.newCourses) ? body.newCourses.map(normalizeCourse) : [];

		if (!name || name.length > 100) throw badRequest('Enter a valid name of at most 100 characters.');
		if (!EMAIL_PATTERN.test(email) || email.length > 255) throw badRequest('Enter a valid email address.');
		const invalidPassword = passwordError(password);
		if (invalidPassword) throw badRequest(invalidPassword);
		if (db.users.some((user) => user.email.toLowerCase() === email.toLowerCase())) {
			throw conflict('Another account already uses this email address.');
		}
		if (!school) throw badRequest('Choose your school.');
		if (!programme || programme.length > 100) throw badRequest('Enter a valid programme of at most 100 characters.');
		if (!Number.isInteger(Number(body.yearOfStudy)) || Number(body.yearOfStudy) < 1 || Number(body.yearOfStudy) > 5) {
			throw badRequest('Year of study must be between 1 and 5.');
		}
		if (!PHONE_PATTERN.test(contactNumber)) throw badRequest('Enter a valid phone number, e.g. +65 9123 4567.');
		const newCourseCodes = newCourses.map((course) => course.code);
		if (new Set(newCourseCodes).size !== newCourseCodes.length) throw conflict('A course with this code already exists.');
		if (newCourses.some((course) => db.courses.some((existing) => existing.code === course.code))) {
			throw conflict('A course with this code already exists.');
		}
		if (!Array.isArray(body.courses) || body.courses.length === 0) throw badRequest('Select at least one course.');
		if (body.courses.some((code) => !db.courses.some((course) => course.code === code) && !newCourseCodes.includes(code))) {
			throw badRequest('One of the selected courses does not exist.');
		}

		const telegramHandle = normalizeTelegramHandle(body.telegramHandle);
		if (telegramHandle === null) throw badRequest(INVALID_HANDLE_MESSAGE);
		const id = nextId('user');
		db.courses.push(...newCourses);
		db.courses.sort((a, b) => a.code.localeCompare(b.code));
		const user = {
			id,
			name,
			email: email.toLowerCase(),
			password,
			role: 'STUDENT',
			status: 'ACTIVE',
			lastLogin: new Date().toISOString(),
			createdAt: new Date().toISOString(),
		};
		db.users.push(user);
		db.students[id] = {
			school,
			programme,
			yearOfStudy: Number(body.yearOfStudy),
			contactNumber,
			telegramHandle: telegramHandle || null,
			courses: [...new Set(body.courses)],
			preferences: [],
		};
		const token = issueSession(db, user);
		return created({ token, user: toSessionUser(user) });
	},
	{ public: true },
);

route(
	'POST',
	'/auth/login',
	({ db, body }) => {
		const user = db.users.find((u) => u.email.toLowerCase() === String(body.email ?? '').trim().toLowerCase());
		if (!user || user.password !== body.password) throw new HttpError(401, 'Invalid email or password.');
		if (user.status === 'SUSPENDED') throw new HttpError(403, 'This account is suspended. Contact an administrator.');
		user.lastLogin = new Date().toISOString();
		const token = issueSession(db, user);
		saveDb();
		return { token, user: toSessionUser(user) };
	},
	{ public: true },
);

route('GET', '/auth/me', ({ user }) => toSessionUser(user));

// Each login creates a separate mock session; logout revokes only that session.
route('POST', '/auth/logout', ({ db, token }) => {
	revokeSession(db, token);
	return noContent();
});

route('GET', '/courses', ({ db }) => db.courses);
route(
	'POST',
	'/courses',
	({ db, body }) => {
		const course = normalizeCourse(body);
		if (db.courses.some((existing) => existing.code === course.code)) throw conflict('A course with this code already exists.');
		db.courses.push(course);
		db.courses.sort((a, b) => a.code.localeCompare(b.code));
		return created(course);
	},
	{ role: 'STUDENT' },
);
route('GET', '/public/courses', ({ db }) => db.courses, { public: true });
