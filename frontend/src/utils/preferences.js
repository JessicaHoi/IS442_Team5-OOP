/**
 * A student has one study preference per course they need a buddy for.
 * These helpers pick the one that applies in a given context.
 */

/** A blank preference for `course`, used when the student adds a new one. */
export function emptyPreference(course) {
	return { course, meetingMode: 'EITHER', groupFormat: 'EITHER', goals: [], availability: [] };
}

/** The preference for `course`; without a course (or no match), the first preference. Null when there are none. */
export function preferenceFor(preferences, course) {
	if (!preferences?.length) return null;
	return preferences.find((preference) => preference.course === course) ?? preferences[0];
}
