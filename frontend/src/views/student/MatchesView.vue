<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { matchesApi } from '../../api/matches.api';
import { studentsApi } from '../../api/students.api';
import { connectionsApi } from '../../api/connections.api';
import { useLookupsStore } from '../../stores/lookups';
import { useToast } from '../../composables/useToast';
import PageHeader from '../../components/ui/PageHeader.vue';
import EmptyState from '../../components/ui/EmptyState.vue';
import LoadingState from '../../components/ui/LoadingState.vue';
import ErrorState from '../../components/ui/ErrorState.vue';
import MessageModal from '../../components/ui/MessageModal.vue';
import MatchFilters from '../../components/matching/MatchFilters.vue';
import MatchCard from '../../components/matching/MatchCard.vue';
import StudentProfileModal from '../../components/matching/StudentProfileModal.vue';

const SEARCH_DEBOUNCE_MS = 250;
const EMPTY_FILTERS = { course: '', goal: '', day: '', meetingMode: '', groupFormat: '' };

const lookups = useLookupsStore();
const toast = useToast();

const filters = reactive({ ...EMPTY_FILTERS });
const results = ref([]);
const loading = ref(true);
const error = ref('');
const me = ref(null);

let latestSearch = 0;
let debounceTimer;

const profileIncomplete = computed(
	() =>
		me.value !== null &&
		(!me.value.preferences.course || me.value.preferences.goals.length === 0 || me.value.preferences.availability.length === 0),
);

async function search() {
	const ticket = ++latestSearch;
	loading.value = true;
	error.value = '';
	try {
		const found = await matchesApi.search({
			course: filters.course,
			goal: filters.goal,
			day: filters.day,
			studyMode: filters.meetingMode,
			groupFormat: filters.groupFormat,
		});
		if (ticket === latestSearch) results.value = found;
	} catch (caught) {
		if (ticket === latestSearch) error.value = caught.message;
	} finally {
		if (ticket === latestSearch) loading.value = false;
	}
}

watch(filters, () => {
	clearTimeout(debounceTimer);
	debounceTimer = setTimeout(search, SEARCH_DEBOUNCE_MS);
});
onBeforeUnmount(() => clearTimeout(debounceTimer));

function resetFilters() {
	// Keep the search course as it is: only the extra filters are cleared.
	Object.assign(filters, { ...EMPTY_FILTERS, course: filters.course, goal: filters.goal });
}

onMounted(async () => {
	try {
		await lookups.loadCourses();
		me.value = await studentsApi.getMe();
	} catch (caught) {
		error.value = caught.message;
		loading.value = false;
		return;
	}
	const defaultCourse = me.value.preferences.course ?? '';
	if (defaultCourse) filters.course = defaultCourse; // triggers the search through the watcher
	else search();
});

// ---- Public profile -------------------------------------------------------
const profileOpen = ref(false);
const profileStudent = ref(null);
const profileLoading = ref(false);
const profileError = ref('');
let requestAfterProfileClosed = null;

async function viewProfile(studentId) {
	profileOpen.value = true;
	profileStudent.value = null;
	profileError.value = '';
	profileLoading.value = true;
	try {
		profileStudent.value = await studentsApi.getPublicProfile(studentId);
	} catch (caught) {
		profileError.value = caught.message;
	} finally {
		profileLoading.value = false;
	}
}

// ---- Buddy request --------------------------------------------------------
const requestOpen = ref(false);
const requestTarget = ref(null);
const sending = ref(false);

function openRequest(student) {
	requestTarget.value = student;
	requestOpen.value = true;
}

/** Two Bootstrap modals cannot overlap, so wait for the profile to finish closing. */
function requestFromProfile(student) {
	requestAfterProfileClosed = student;
	profileOpen.value = false;
}

function onProfileHidden() {
	if (requestAfterProfileClosed) {
		openRequest(requestAfterProfileClosed);
		requestAfterProfileClosed = null;
	}
}

async function sendRequest(message) {
	const target = requestTarget.value;
	sending.value = true;
	try {
		await connectionsApi.send(target.id, message);
		const match = results.value.find((item) => item.student.id === target.id);
		if (match) match.connectionStatus = 'PENDING_SENT';
		requestOpen.value = false;
		toast.success(`Request sent to ${target.name}.`);
	} catch (caught) {
		toast.error(caught.message);
	} finally {
		sending.value = false;
	}
}
</script>

<template>
	<div>
		<PageHeader title="Find study buddies" subtitle="Students ranked by how well they match your courses, timetable and preferences." />

		<div v-if="profileIncomplete" class="alert alert-info d-flex flex-wrap align-items-center justify-content-between gap-2" role="status">
			<span><i class="bi bi-info-circle me-2"></i>Complete your study preferences to get more accurate match scores.</span>
			<RouterLink class="btn btn-sm btn-primary" to="/profile">Update profile</RouterLink>
		</div>

		<MatchFilters :model-value="filters" class="mb-4" @update:model-value="Object.assign(filters, $event)" @reset="resetFilters" />

		<ErrorState v-if="error" :message="error" @retry="search" />
		<LoadingState v-else-if="loading" label="Finding matches…" />
		<EmptyState
			v-else-if="results.length === 0"
			icon="bi-search"
			title="No students match these filters"
			message="Try a different course or goal, or clear some filters."
		/>
		<template v-else>
			<p class="text-muted small mb-3">{{ results.length }} {{ results.length === 1 ? 'student' : 'students' }} found</p>
			<div class="d-flex flex-column gap-3">
				<MatchCard
					v-for="(match, index) in results"
					:key="match.student.id"
					:match="match"
					:rank="index + 1"
					@view="viewProfile"
					@request="openRequest"
				/>
			</div>
		</template>

		<StudentProfileModal
			v-model="profileOpen"
			:student="profileStudent"
			:loading="profileLoading"
			:error="profileError"
			@request="requestFromProfile"
			@hidden="onProfileHidden"
		/>
		<MessageModal
			v-model="requestOpen"
			title="Send study-buddy request"
			:subtitle="requestTarget ? `Introduce yourself to ${requestTarget.name}.` : ''"
			placeholder="Hi! I’d love to study together for…"
			:busy="sending"
			@submit="sendRequest"
		/>
	</div>
</template>
