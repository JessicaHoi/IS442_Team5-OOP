import { http, unwrap } from './http';

export const groupsApi = {
	list: (course) => unwrap(http.get('/groups', { params: course ? { course } : {} })),
	mine: () => unwrap(http.get('/groups/mine')),
	get: (id) => unwrap(http.get(`/groups/${id}`)),
	create: (group) => unwrap(http.post('/groups', group)),
	update: (id, group) => unwrap(http.put(`/groups/${id}`, group)),
	leadership: (id) => unwrap(http.get(`/groups/${id}/leadership`)),
	transferLeadership: (id, studentId) => unwrap(http.put(`/groups/${id}/leader`, { studentId })),
	close: (id) => unwrap(http.post(`/groups/${id}/close`)),
	bestTimes: (id) => unwrap(http.get(`/groups/${id}/best-times`)),
	myAvailability: (id) => unwrap(http.get(`/groups/${id}/availability/me`)),
	saveAvailability: (id, payload) => unwrap(http.put(`/groups/${id}/availability/me`, payload)),
	resetAvailability: (id, revision) => unwrap(http.delete(`/groups/${id}/availability/me`, { params: { revision } })),

	requestToJoin: (id, message) => unwrap(http.post(`/groups/${id}/join-requests`, { message })),
	joinRequests: (id) => unwrap(http.get(`/groups/${id}/join-requests`)),
	acceptRequest: (id, requestId) => unwrap(http.post(`/groups/${id}/join-requests/${requestId}/accept`)),
	rejectRequest: (id, requestId) => unwrap(http.post(`/groups/${id}/join-requests/${requestId}/reject`)),
	removeMember: (id, studentId) => unwrap(http.delete(`/groups/${id}/members/${studentId}`)),
	leave: (id) => unwrap(http.post(`/groups/${id}/leave`)),
	setChatLink: (id, url) => unwrap(http.put(`/groups/${id}/chat-link`, { url })),
	clearChatLink: (id) => unwrap(http.delete(`/groups/${id}/chat-link`)),

	/** The group's Telegram chat, as the signed-in member sees it. */
	chat: (id) => unwrap(http.get(`/groups/${id}/chat`)),
	/** Returns a link the leader opens in Telegram to choose or create the chat. */
	startChatSetup: (id) => unwrap(http.post(`/groups/${id}/chat/setup`)),
	verifyChat: (id) => unwrap(http.post(`/groups/${id}/chat/verify`)),
	/** `renew` replaces the invite, or removes whoever joined with it ("Not you?"). */
	chatInvite: (id, renew = false) => unwrap(http.post(`/groups/${id}/chat/invite`, { renew })),
	unlinkChat: (id) => unwrap(http.delete(`/groups/${id}/chat`)),
};
