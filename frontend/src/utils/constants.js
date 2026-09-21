/**
 * Single source of truth for enums, labels and defaults.
 * The string values mirror the enums the backend API is expected to use.
 */

export const ROLES = Object.freeze({ STUDENT: 'STUDENT', ADMIN: 'ADMIN' });

export const USER_STATUSES = [
	{ value: 'ACTIVE', label: 'Active' },
	{ value: 'SUSPENDED', label: 'Suspended' },
];

export const MEETING_MODES = [
	{ value: 'IN_PERSON', label: 'In-person' },
	{ value: 'ONLINE', label: 'Online' },
	{ value: 'EITHER', label: 'Either' },
];

export const GROUP_FORMATS = [
	{ value: 'ONE_TO_ONE', label: 'One-to-one' },
	{ value: 'SMALL_GROUP', label: 'Small group' },
	{ value: 'EITHER', label: 'Either' },
];

export const STUDY_GOALS = [
	{ value: 'CONCEPT_REVIEW', label: 'Concept review' },
	{ value: 'PROBLEM_SOLVING', label: 'Problem solving' },
	{ value: 'EXAM_PREPARATION', label: 'Exam preparation' },
	{ value: 'PROJECT_DISCUSSION', label: 'Project discussion' },
];

export const SCHOOLS = [
	'School of Computing and Information Systems',
	'Lee Kong Chian School of Business',
	'School of Accountancy',
	'School of Economics',
	'School of Social Sciences',
];

export const YEARS_OF_STUDY = [1, 2, 3, 4, 5];

export const DAYS = [
	{ value: 'MON', label: 'Mon', long: 'Monday' },
	{ value: 'TUE', label: 'Tue', long: 'Tuesday' },
	{ value: 'WED', label: 'Wed', long: 'Wednesday' },
	{ value: 'THU', label: 'Thu', long: 'Thursday' },
	{ value: 'FRI', label: 'Fri', long: 'Friday' },
	{ value: 'SAT', label: 'Sat', long: 'Saturday' },
	{ value: 'SUN', label: 'Sun', long: 'Sunday' },
];

export const MATCHING_STRATEGIES = [
	{
		value: 'BALANCED',
		label: 'Balanced',
		icon: 'bi-sliders',
		description: 'Considers every enabled criterion using the weights below.',
	},
	{
		value: 'AVAILABILITY_FIRST',
		label: 'Availability-First',
		icon: 'bi-calendar-check',
		description: 'Ranks the strongest timetable overlap first, then the other criteria.',
	},
	{
		value: 'COURSE_FIRST',
		label: 'Course-First',
		icon: 'bi-book',
		description: 'Ranks exact course alignment first, then the remaining preferences.',
	},
];

/** Matching criteria the administrator can configure. `key` matches the API. */
export const MATCHING_CRITERIA = [
	{ key: 'course', label: 'Course match', hint: 'Both students take the same course.' },
	{ key: 'availability', label: 'Availability overlap', hint: 'Hours of shared weekly time slots.' },
	{ key: 'studyMode', label: 'Meeting mode', hint: 'In-person / online compatibility.' },
	{ key: 'studyGoal', label: 'Study goal', hint: 'Shared goals such as exam preparation.' },
	{ key: 'groupSize', label: 'Preferred group size', hint: 'One-to-one / small group compatibility.' },
];

/** Used by "Reset to defaults" on the matching configuration page and by the mock. */
export const DEFAULT_MATCHING_CONFIG = Object.freeze({
	strategy: 'BALANCED',
	criteria: [
		{ key: 'course', enabled: true, weight: 30 },
		{ key: 'availability', enabled: true, weight: 25 },
		{ key: 'studyMode', enabled: true, weight: 15 },
		{ key: 'studyGoal', enabled: true, weight: 20 },
		{ key: 'groupSize', enabled: true, weight: 10 },
	],
});

export const CONNECTION_STATUS = Object.freeze({
	NONE: 'NONE',
	PENDING_SENT: 'PENDING_SENT',
	PENDING_RECEIVED: 'PENDING_RECEIVED',
	CONNECTED: 'CONNECTED',
});

export const MIN_GROUP_SIZE = 2;
export const MAX_GROUP_SIZE = 20;
export const MESSAGE_MAX_LENGTH = 300;

/** Look up the display label for an enum value. */
export function labelOf(options, value) {
	return options.find((option) => option.value === value)?.label ?? value ?? '';
}
