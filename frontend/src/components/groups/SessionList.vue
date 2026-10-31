<script setup>
import { computed, ref, watch } from 'vue';
import { groupsApi } from '../../api/groups.api';
import { sessionsApi } from '../../api/sessions.api';
import { useConfirm } from '../../composables/useConfirm';
import { useToast } from '../../composables/useToast';
import { calendarFileName, googleCalendarUrl, outlookCalendarUrl, sessionTimesFromSlot } from '../../utils/calendar';
import { labelOf, MEETING_MODES } from '../../utils/constants';
import { formatSessionTime } from '../../utils/format';
import EmptyState from '../ui/EmptyState.vue';
import ErrorState from '../ui/ErrorState.vue';
import LoadingState from '../ui/LoadingState.vue';
import SessionFormModal from './SessionFormModal.vue';

/** A group's study sessions: everyone can answer and export them, the leader can also schedule and change them. */
const props = defineProps({
	groupId: { type: String, required: true },
	/** Show the schedule, change and cancel actions. */
	canManage: Boolean,
	/** A best-time suggestion to open the form with. */
	prefill: { type: Object, default: null },
});
const emit = defineEmits(['prefill-used']);

const toast = useToast();
const { confirm } = useConfirm();

const upcoming = ref([]);
const past = ref([]);
const loading = ref(false);
const error = ref('');
const busyId = ref(null);
const formOpen = ref(false);
const editing = ref(null);
const formDefaults = ref(null);
const saving = ref(false);
const chatActive = ref(false);

const isEmpty = computed(() => !upcoming.value.length && !past.value.length);

async function load() {
	loading.value = true;
	error.value = '';
	try {
		[upcoming.value, past.value] = await Promise.all([sessionsApi.list(props.groupId, 'upcoming'), sessionsApi.list(props.groupId, 'past')]);
	} catch (caught) {
		error.value = caught.message;
	} finally {
		loading.value = false;
	}
}

/** Only decides whether to offer the video chat, so a failure just leaves it out. */
async function loadChatStatus() {
	if (!props.canManage) return;
	chatActive.value = await groupsApi.chat(props.groupId).then((chat) => chat.status === 'ACTIVE').catch(() => false);
}

watch(() => props.groupId, () => {
	load();
	loadChatStatus();
}, { immediate: true });
watch(() => props.prefill, (slot) => {
	if (!slot) return;
	openForm(null, sessionTimesFromSlot(slot));
	emit('prefill-used');
}, { immediate: true });

const canAnswer = (session) => session.status === 'SCHEDULED' && new Date(session.startsAt) > new Date();
const going = (session) => session.attendees.filter((attendee) => attendee.response === 'GOING').map((attendee) => attendee.name);

function openForm(session = null, defaults = null) {
	editing.value = session;
	formDefaults.value = defaults;
	formOpen.value = true;
}

async function save(payload) {
	saving.value = true;
	try {
		if (editing.value) await sessionsApi.update(props.groupId, editing.value.id, payload);
		else await sessionsApi.create(props.groupId, payload);
		toast.success(editing.value ? 'Session updated.' : 'Session scheduled.');
		formOpen.value = false;
		await load();
	} catch (caught) {
		toast.error(caught.message);
	} finally {
		saving.value = false;
	}
}

async function cancelSession(session) {
	const confirmed = await confirm({
		title: `Cancel “${session.title}”?`,
		message: 'Members will see it as cancelled. This cannot be undone.',
		confirmLabel: 'Cancel session',
		danger: true,
	});
	if (!confirmed) return;
	await act(session, async () => {
		await sessionsApi.cancel(props.groupId, session.id);
		toast.success('Session cancelled.');
		await load();
	});
}

function respond(session, response) {
	return act(session, async () => {
		await sessionsApi.respond(props.groupId, session.id, response);
		await load();
	});
}

function downloadCalendar(session) {
	return act(session, async () => {
		const text = await sessionsApi.calendarFile(props.groupId, session.id);
		const url = URL.createObjectURL(new Blob([text], { type: 'text/calendar;charset=utf-8' }));
		const link = document.createElement('a');
		link.href = url;
		link.download = calendarFileName(session.title);
		link.click();
		URL.revokeObjectURL(url);
	});
}

async function act(session, action) {
	if (busyId.value) return;
	busyId.value = session.id;
	try {
		await action();
	} catch (caught) {
		toast.error(caught.message);
	} finally {
		busyId.value = null;
	}
}
</script>

<template>
	<section class="py-3" aria-label="Study sessions">
		<div v-if="canManage" class="d-flex justify-content-end mb-3">
			<button type="button" class="btn btn-primary btn-sm" @click="openForm()"><i class="bi bi-calendar-plus me-2"></i>Schedule a session</button>
		</div>

		<LoadingState v-if="loading && isEmpty" />
		<ErrorState v-else-if="error" :message="error" @retry="load" />
		<EmptyState
			v-else-if="isEmpty"
			icon="bi-calendar-event"
			title="No sessions yet"
			:message="canManage ? 'Schedule one here, or pick a time under Details › Best time to meet.' : 'Your group leader has not scheduled a session yet.'"
		/>
		<template v-else>
			<template v-for="section in [{ title: 'Upcoming', items: upcoming }, { title: 'Past', items: past }]" :key="section.title">
				<template v-if="section.items.length">
					<h2 class="h6 text-body-secondary mb-2">{{ section.title }}</h2>
					<ul class="list-group list-group-flush mb-4">
						<li v-for="session in section.items" :key="session.id" class="list-group-item px-0">
							<div class="d-flex flex-wrap justify-content-between gap-2">
								<div>
									<div class="fw-medium" :class="{ 'text-decoration-line-through': session.status === 'CANCELLED' }">
										{{ session.title }}
										<span v-if="session.status === 'CANCELLED'" class="badge text-bg-secondary ms-1">Cancelled</span>
									</div>
									<div class="small text-muted">{{ formatSessionTime(session.startsAt, session.endsAt) }}</div>
									<div class="small text-muted">{{ labelOf(MEETING_MODES, session.meetingMode) }}<template v-if="session.location"> · {{ session.location }}</template></div>
									<div v-if="session.groupVideoChat" class="small text-muted"><i class="bi bi-camera-video me-1"></i>Video chat in the group's Telegram chat</div>
									<div v-if="session.note" class="small mt-1">{{ session.note }}</div>
									<div class="small text-muted mt-1">
										{{ session.goingCount }} going · {{ session.notGoingCount }} can't make it
										<template v-if="going(session).length"> · {{ going(session).join(', ') }}</template>
									</div>
								</div>
								<div class="d-flex flex-wrap align-items-start gap-2">
									<a v-if="session.meetingLink && session.status === 'SCHEDULED' && section.title === 'Upcoming'" class="btn btn-sm btn-primary" :href="session.meetingLink" target="_blank" rel="noopener noreferrer"><i class="bi bi-camera-video me-1"></i>Join</a>
									<template v-if="canAnswer(session)">
										<button type="button" class="btn btn-sm" :class="session.myRsvp === 'GOING' ? 'btn-success' : 'btn-outline-success'" :aria-pressed="session.myRsvp === 'GOING'" :disabled="busyId === session.id" @click="respond(session, 'GOING')">Going</button>
										<button type="button" class="btn btn-sm" :class="session.myRsvp === 'NOT_GOING' ? 'btn-secondary' : 'btn-outline-secondary'" :aria-pressed="session.myRsvp === 'NOT_GOING'" :disabled="busyId === session.id" @click="respond(session, 'NOT_GOING')">Can't make it</button>
										<div class="dropdown">
											<button type="button" class="btn btn-sm btn-light border dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false"><i class="bi bi-calendar-plus me-1"></i>Add to calendar</button>
											<ul class="dropdown-menu">
												<li><a class="dropdown-item" :href="googleCalendarUrl(session)" target="_blank" rel="noopener noreferrer">Google Calendar</a></li>
												<li><a class="dropdown-item" :href="outlookCalendarUrl(session)" target="_blank" rel="noopener noreferrer">Outlook</a></li>
												<li><button type="button" class="dropdown-item" @click="downloadCalendar(session)">Apple or other (.ics)</button></li>
											</ul>
										</div>
									</template>
									<button v-else-if="session.status === 'CANCELLED' && section.title === 'Upcoming'" type="button" class="btn btn-sm btn-light border" :disabled="busyId === session.id" @click="downloadCalendar(session)">Update my calendar (.ics)</button>
									<template v-if="canManage && canAnswer(session)">
										<button type="button" class="btn btn-sm btn-light border" @click="openForm(session)">Change</button>
										<button type="button" class="btn btn-sm btn-outline-danger" :disabled="busyId === session.id" @click="cancelSession(session)">Cancel</button>
									</template>
								</div>
							</div>
						</li>
					</ul>
				</template>
			</template>
		</template>

		<SessionFormModal v-model="formOpen" :session="editing" :defaults="formDefaults" :saving="saving" :can-use-video-chat="chatActive" @submit="save" />
	</section>
</template>
