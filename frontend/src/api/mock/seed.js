/**
 * Deterministic demo data: 10 courses, 50 student profiles, 1 administrator,
 * plus a few connections, groups and join requests so every screen has content.
 */
import { DAYS, DEFAULT_MATCHING_CONFIG, SCHOOLS } from '../../utils/constants';

export const DEMO_PASSWORD = 'password';
/** The featured demo student (Aisha Rahman); only placed in groups explicitly. */
const DEMO_STUDENT_ID = 2;

export const COURSES = [
	{ code: 'IS112', name: 'Data Management', school: SCHOOLS[0] },
	{ code: 'IS113', name: 'Web Application Development I', school: SCHOOLS[0] },
	{ code: 'IS210', name: 'Business Process Analytics', school: SCHOOLS[0] },
	{ code: 'IS212', name: 'Software Project Management', school: SCHOOLS[0] },
	{ code: 'IS216', name: 'Web Application Development II', school: SCHOOLS[0] },
	{ code: 'IS442', name: 'Object-Oriented Application Development', school: SCHOOLS[0] },
	{ code: 'STAT101', name: 'Introductory Statistics', school: SCHOOLS[3] },
	{ code: 'ACCT101', name: 'Financial Accounting', school: SCHOOLS[2] },
	{ code: 'ECON101', name: 'Principles of Microeconomics', school: SCHOOLS[3] },
	{ code: 'MKTG101', name: 'Marketing Fundamentals', school: SCHOOLS[1] },
];

const NAMES = [
	'Aisha Rahman', 'Wei Jie Tan', 'Priya Nair', 'Marcus Lim', 'Nurul Huda',
	'Jun Hao Lee', 'Sarah Chen', 'Daniel Ong', 'Kavya Iyer', 'Ethan Goh',
	'Mei Ling Wong', 'Rizwan Ahmad', 'Chloe Tan', 'Arjun Menon', 'Hui Min Koh',
	'Isaac Teo', 'Farah Ismail', 'Brandon Ng', 'Yuki Tanaka', 'Sophia Lau',
	'Ravi Kumar', 'Jia Hui Chua', 'Nathan Yeo', 'Amirah Yusof', 'Zhi Wei Sim',
	'Grace Ho', 'Hafiz Salleh', 'Tiffany Ang', 'Vikram Singh', 'Eunice Lee',
	'Darren Chong', 'Siti Aminah', 'Ryan Phua', 'Lakshmi Pillai', 'Kelvin Toh',
	'Natalie Foo', 'Irfan Hakim', 'Cheryl Seah', 'Benedict Lim', 'Anjali Sharma',
	'Jonathan Wee', 'Sabrina Tay', 'Haziq Rosli', 'Olivia Kwek', 'Shawn Ler',
	'Deepa Krishnan', 'Timothy Low', 'Rachel Soh', 'Mohammed Faiz', 'Xin Yi Lim',
];

const SCHOOL_PROFILES = [
	{
		weight: 55,
		school: SCHOOLS[0],
		programmes: ['Information Systems', 'Computer Science', 'Software Engineering', 'Computing and Law'],
		coursePool: ['IS112', 'IS113', 'IS210', 'IS212', 'IS216', 'IS442', 'IS442', 'IS442', 'STAT101'],
	},
	{
		weight: 20,
		school: SCHOOLS[1],
		programmes: ['Business Management', 'Marketing'],
		coursePool: ['MKTG101', 'ACCT101', 'ECON101', 'STAT101', 'IS210'],
	},
	{
		weight: 10,
		school: SCHOOLS[2],
		programmes: ['Accountancy'],
		coursePool: ['ACCT101', 'ECON101', 'STAT101', 'MKTG101'],
	},
	{
		weight: 10,
		school: SCHOOLS[3],
		programmes: ['Economics'],
		coursePool: ['ECON101', 'STAT101', 'ACCT101', 'MKTG101'],
	},
	{
		weight: 5,
		school: SCHOOLS[4],
		programmes: ['Psychology'],
		coursePool: ['STAT101', 'ECON101', 'MKTG101'],
	},
];

const DAY_MS = 86_400_000;
const SLOT_STARTS = ['10:00', '13:00', '14:00', '16:00', '19:00'];
const SLOT_HOURS = [1, 2, 2, 3];
const SUSPENDED_INDEXES = [47, 48, 49];

/** Small seeded PRNG so the demo data is identical on every load. */
function createRandom(seed) {
	let state = seed;
	return () => {
		state = (state + 0x6d2b79f5) | 0;
		let t = Math.imul(state ^ (state >>> 15), 1 | state);
		t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
		return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
	};
}

function pad(number) {
	return String(number).padStart(2, '0');
}

export function createSeedData() {
	const random = createRandom(442);
	const pick = (items) => items[Math.floor(random() * items.length)];
	// Fisher-Yates shuffle: unlike sort-with-random-comparator it gives the same
	// result in every JavaScript engine, so the demo data is truly deterministic.
	const sample = (items, count) => {
		const copy = [...items];
		for (let i = copy.length - 1; i > 0; i--) {
			const j = Math.floor(random() * (i + 1));
			[copy[i], copy[j]] = [copy[j], copy[i]];
		}
		return copy.slice(0, count);
	};
	const pickWeighted = (entries) => {
		let roll = random() * entries.reduce((sum, entry) => sum + entry.weight, 0);
		return entries.find((entry) => (roll -= entry.weight) < 0) ?? entries[0];
	};
	const daysAgo = (days) => new Date(Date.now() - days * DAY_MS).toISOString();

	function randomAvailability() {
		const days = sample(DAYS.slice(0, 6), 2 + Math.floor(random() * 3));
		return days.map((day) => {
			const start = pick(SLOT_STARTS);
			const endHour = Number(start.slice(0, 2)) + pick(SLOT_HOURS);
			return { day: day.value, start, end: `${pad(Math.min(endHour, 22))}:00` };
		});
	}

	function randomPhoneNumber() {
		const digits = String(Math.floor(10_000_000 + random() * 89_999_999));
		return `+65 ${pick(['8', '9'])}${digits.slice(1, 4)} ${digits.slice(4, 8)}`;
	}

	function randomStudent() {
		const profile = pickWeighted(SCHOOL_PROFILES);
		const courses = [...new Set(sample(profile.coursePool, 3 + Math.floor(random() * 3)))];
		const meetingMode = pickWeighted([
			{ weight: 40, value: 'IN_PERSON' },
			{ weight: 25, value: 'ONLINE' },
			{ weight: 35, value: 'EITHER' },
		]).value;
		const groupFormat = pickWeighted([
			{ weight: 30, value: 'ONE_TO_ONE' },
			{ weight: 40, value: 'SMALL_GROUP' },
			{ weight: 30, value: 'EITHER' },
		]).value;
		return {
			school: profile.school,
			programme: pick(profile.programmes),
			yearOfStudy: 1 + Math.floor(random() * 4),
			contactNumber: randomPhoneNumber(),
			courses,
			preferences: {
				course: pick(courses),
				meetingMode,
				groupFormat,
				goals: sample(['CONCEPT_REVIEW', 'PROBLEM_SOLVING', 'EXAM_PREPARATION', 'PROJECT_DISCUSSION'], 1 + Math.floor(random() * 3)),
				availability: randomAvailability(),
			},
		};
	}

	// ---- Accounts and student profiles -------------------------------------
	const users = [
		{
			id: 1,
			name: 'System Admin',
			email: 'admin@smu.edu.sg',
			password: DEMO_PASSWORD,
			role: 'ADMIN',
			status: 'ACTIVE',
			lastLogin: daysAgo(0),
			createdAt: daysAgo(120),
		},
	];
	const students = {};

	NAMES.forEach((name, index) => {
		const id = index + 2;
		const student = randomStudent();

		// Students 1-8 are IS442 students so the demo user has plenty of candidates.
		if (index >= 1 && index <= 8) {
			student.school = SCHOOLS[0];
			student.programme = 'Information Systems';
			student.courses = [...new Set(['IS442', ...student.courses.filter((code) => code.startsWith('IS') || code === 'STAT101')])];
			student.preferences.course = 'IS442';
		}
		students[id] = student;
		users.push({
			id,
			name,
			email: `${name.toLowerCase().replace(/\s+/g, '.')}@smu.edu.sg`,
			password: DEMO_PASSWORD,
			role: 'STUDENT',
			status: SUSPENDED_INDEXES.includes(index) ? 'SUSPENDED' : 'ACTIVE',
			lastLogin: index % 7 === 0 ? null : daysAgo(1 + (index % 20)),
			createdAt: daysAgo(30 + index),
		});
	});

	// Featured demo pair, modelled on Appendix A of the project brief.
	students[2] = {
		...students[2],
		school: SCHOOLS[0],
		programme: 'Information Systems',
		yearOfStudy: 2,
		contactNumber: '+65 9123 4567',
		courses: ['IS442', 'IS212', 'IS216', 'STAT101'],
		preferences: {
			course: 'IS442',
			meetingMode: 'IN_PERSON',
			groupFormat: 'SMALL_GROUP',
			goals: ['EXAM_PREPARATION', 'PROBLEM_SOLVING'],
			availability: [
				{ day: 'WED', start: '19:00', end: '21:00' },
				{ day: 'FRI', start: '14:00', end: '16:00' },
			],
		},
	};
	students[3] = {
		...students[3],
		school: SCHOOLS[0],
		programme: 'Computer Science',
		yearOfStudy: 2,
		courses: ['IS442', 'IS212', 'IS113'],
		preferences: {
			course: 'IS442',
			meetingMode: 'EITHER',
			groupFormat: 'EITHER',
			goals: ['EXAM_PREPARATION', 'PROBLEM_SOLVING'],
			availability: [
				{ day: 'WED', start: '19:00', end: '21:00' },
				{ day: 'THU', start: '18:00', end: '20:00' },
				{ day: 'FRI', start: '14:00', end: '16:00' },
			],
		},
	};

	// ---- Connections ------------------------------------------------------
	const connections = [];
	const addConnection = (fromIndex, toIndex, status, message, ageInDays) => {
		connections.push({
			id: connections.length + 1,
			fromId: fromIndex + 2,
			toId: toIndex + 2,
			message,
			status,
			createdAt: daysAgo(ageInDays),
			respondedAt: status === 'PENDING' ? null : daysAgo(Math.max(0, ageInDays - 1)),
		});
	};
	addConnection(0, 2, 'ACCEPTED', 'Hi! Want to revise IS442 together?', 6);
	addConnection(4, 0, 'PENDING', 'Hi Aisha, I am also preparing for the IS442 exam. Free on Wednesdays?', 1);
	addConnection(6, 0, 'PENDING', 'Looking for someone to go through past-year papers with.', 2);
	addConnection(0, 5, 'PENDING', 'Hello! Would you like to be study buddies for IS442?', 1);
	[[10, 11], [12, 13], [14, 15], [16, 17], [18, 19], [20, 21]].forEach(([from, to], i) =>
		addConnection(from, to, 'ACCEPTED', '', 10 + i),
	);
	addConnection(22, 23, 'PENDING', '', 3);
	addConnection(24, 25, 'PENDING', '', 4);

	// ---- Study groups -----------------------------------------------------
	const studentIdsTaking = (code) =>
		Object.keys(students)
			.map(Number)
			.filter((id) => id !== DEMO_STUDENT_ID && students[id].courses.includes(code));

	const groups = [];
	const joinRequests = [];
	const addGroup = ({ name, description, courseCode, leaderId, memberIds, extraMembers, requesterIds = [], ...rest }) => {
		const taken = new Set([leaderId, ...memberIds, ...requesterIds]);
		const filler = studentIdsTaking(courseCode)
			.filter((id) => !taken.has(id))
			.slice(0, extraMembers);
		const group = {
			id: groups.length + 1,
			name,
			description,
			courseCode,
			leaderId,
			memberIds: [leaderId, ...memberIds, ...filler],
			status: 'OPEN',
			createdAt: daysAgo(14),
			...rest,
		};
		groups.push(group);
		requesterIds.forEach((studentId) =>
			joinRequests.push({
				id: joinRequests.length + 1,
				groupId: group.id,
				studentId,
				message: 'Hi, I would love to join your group!',
				status: 'PENDING',
				createdAt: daysAgo(1),
			}),
		);
		return group;
	};

	addGroup({
		name: 'IS442 Exam Crew',
		description: 'Weekly past-year paper drills and OOP design discussions before the final exam.',
		courseCode: 'IS442',
		leaderId: 2,
		memberIds: [4],
		extraMembers: 1,
		requesterIds: [3, 9],
		goals: ['EXAM_PREPARATION', 'PROBLEM_SOLVING'],
		meetingMode: 'IN_PERSON',
		availability: [{ day: 'WED', start: '19:00', end: '21:00' }],
		maxSize: 5,
	});
	addGroup({
		name: 'Web Dev Study Circle',
		description: 'Pair up on project milestones and review each other’s Vue and Spring Boot code.',
		courseCode: 'IS216',
		leaderId: studentIdsTaking('IS216').find((id) => id !== 2),
		memberIds: [2],
		extraMembers: 2,
		goals: ['PROJECT_DISCUSSION', 'CONCEPT_REVIEW'],
		meetingMode: 'ONLINE',
		availability: [{ day: 'TUE', start: '19:00', end: '21:00' }, { day: 'SAT', start: '14:00', end: '16:00' }],
		maxSize: 6,
	});
	addGroup({
		name: 'SQL Drills',
		description: 'Short weekly sessions solving query and normalisation problems together.',
		courseCode: 'IS112',
		leaderId: studentIdsTaking('IS112')[0],
		memberIds: [],
		extraMembers: 2,
		goals: ['PROBLEM_SOLVING'],
		meetingMode: 'EITHER',
		availability: [{ day: 'THU', start: '16:00', end: '18:00' }],
		maxSize: 8,
	});
	addGroup({
		name: 'Micro Mondays',
		description: 'Concept review of the week’s microeconomics lectures over coffee.',
		courseCode: 'ECON101',
		leaderId: studentIdsTaking('ECON101')[0],
		memberIds: [],
		extraMembers: 2,
		goals: ['CONCEPT_REVIEW'],
		meetingMode: 'IN_PERSON',
		availability: [{ day: 'MON', start: '13:00', end: '15:00' }],
		maxSize: 4,
	});
	addGroup({
		name: 'Stats Survivors',
		description: 'A tight group working through problem sets before each quiz.',
		courseCode: 'STAT101',
		leaderId: studentIdsTaking('STAT101')[0],
		memberIds: [],
		extraMembers: 2,
		goals: ['PROBLEM_SOLVING', 'EXAM_PREPARATION'],
		meetingMode: 'EITHER',
		availability: [{ day: 'FRI', start: '10:00', end: '12:00' }],
		maxSize: 3,
	});
	const closed = addGroup({
		name: 'Accounting Night Owls',
		description: 'Late-night revision for the mid-term (now finished).',
		courseCode: 'ACCT101',
		leaderId: studentIdsTaking('ACCT101')[0],
		memberIds: [],
		extraMembers: 1,
		goals: ['EXAM_PREPARATION'],
		meetingMode: 'ONLINE',
		availability: [{ day: 'SUN', start: '19:00', end: '22:00' }],
		maxSize: 5,
	});
	closed.status = 'CLOSED';

	return {
		nextIds: {
			user: users.length + 1,
			connection: connections.length + 1,
			group: groups.length + 1,
			joinRequest: joinRequests.length + 1,
		},
		courses: COURSES,
		users,
		students,
		connections,
		groups,
		joinRequests,
		matchingConfig: structuredClone(DEFAULT_MATCHING_CONFIG),
	};
}
