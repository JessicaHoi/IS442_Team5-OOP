/** Study session rules that the group and admin handlers also need. */

const isUpcoming = (session) => new Date(session.startsAt) > new Date();

export function cancelUpcomingSessions(db, groupId) {
	db.studySessions
		.filter((session) => session.groupId === groupId && session.status === 'SCHEDULED' && isUpcoming(session))
		.forEach((session) => {
			session.status = 'CANCELLED';
			session.icsSequence += 1;
		});
}

export function removeUpcomingRsvps(db, groupId, studentId) {
	db.studySessions
		.filter((session) => session.groupId === groupId && isUpcoming(session))
		.forEach((session) => {
			session.rsvps = session.rsvps.filter((rsvp) => rsvp.studentId !== studentId);
		});
}

export function removeAllRsvps(db, studentId) {
	db.studySessions.forEach((session) => {
		session.rsvps = session.rsvps.filter((rsvp) => rsvp.studentId !== studentId);
	});
}
