import { badRequest, conflict, forbidden, notFound } from '../errors';
import { route } from '../router';
import { buildGroupHeatmap, mergeAvailability } from '../../../utils/groupAvailability';
import { validateSlots } from '../../../utils/availability';
import { DAYS } from '../../../utils/constants';

function memberGroup(db, user, id) {
	const group = db.groups.find((item) => item.id === Number(id));
	if (!group) throw notFound('Group not found.');
	if (!group.memberIds.includes(user.id)) throw forbidden('Only group members can access group availability.');
	return group;
}

function response(db, group, studentId) {
	const override = group.availabilityOverrides?.[studentId];
	const fallback = db.students[studentId]?.preferences.find((preference) => preference.course === group.courseCode)?.availability ?? [];
	return { source: override ? 'GROUP' : 'PROFILE', revision: override?.revision ?? null, availability: override ? override.availability : fallback };
}

function editableGroup(db, user, id, revision) {
	const group = memberGroup(db, user, id);
	if (group.status === 'CLOSED') throw conflict('This group is closed. Availability cannot be changed.');
	if ((group.availabilityOverrides?.[user.id]?.revision ?? null) !== (revision ?? null)) throw conflict('Your availability changed in another session. Reload it before saving again.');
	return group;
}

route('GET', '/groups/:id/best-times', ({ db, user, params }) => {
	const group = memberGroup(db, user, params.id);
	return buildGroupHeatmap(group.memberIds.map((id) => ({ id, name: db.users.find((member) => member.id === id).name, availability: response(db, group, id).availability })));
}, { role: 'STUDENT' });

route('GET', '/groups/:id/availability/me', ({ db, user, params }) => response(db, memberGroup(db, user, params.id), user.id), { role: 'STUDENT' });

route('PUT', '/groups/:id/availability/me', ({ db, user, params, body }) => {
	const slots = body.availability;
	if (!Array.isArray(slots) || slots.length > 512 || slots.some((slot) => !slot || !DAYS.some((day) => day.value === slot.day)
		|| !/^([01]\d|2[0-3]):[0-5]\d$/.test(slot.start) || !/^([01]\d|2[0-3]):[0-5]\d$/.test(slot.end)) || validateSlots(slots).some(Boolean)) {
		throw badRequest('Provide valid, non-overlapping availability slots.');
	}
	const group = editableGroup(db, user, params.id, body.revision);
	group.availabilityOverrides ??= {};
	group.availabilityOverrides[user.id] = { revision: crypto.randomUUID(), availability: mergeAvailability(slots) };
	return response(db, group, user.id);
}, { role: 'STUDENT' });

route('DELETE', '/groups/:id/availability/me', ({ db, user, params, query }) => {
	if (!query.revision) throw badRequest('Provide the current availability revision.');
	const group = editableGroup(db, user, params.id, query.revision);
	delete group.availabilityOverrides?.[user.id];
	return response(db, group, user.id);
}, { role: 'STUDENT' });
