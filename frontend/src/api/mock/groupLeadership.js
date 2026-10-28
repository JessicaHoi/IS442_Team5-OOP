import { badRequest } from './errors';

export function ledGroupView(db, group) {
	return {
		id: group.id,
		name: group.name,
		status: group.status,
		eligibleMembers: db.users.filter((user) => group.memberIds.includes(user.id) && user.id !== group.leaderId && user.status === 'ACTIVE' && user.role === 'STUDENT')
			.map(({ id, name }) => ({ id, name })),
	};
}

export function transferGroupLeadership(db, group, studentId) {
	const successor = ledGroupView(db, group).eligibleMembers.find((member) => member.id === Number(studentId));
	if (!successor) throw badRequest('Choose another active member as the new group leader.');
	group.leaderId = successor.id;
	return ledGroupView(db, group);
}
