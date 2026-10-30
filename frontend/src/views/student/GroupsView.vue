<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { groupsApi } from '../../api/groups.api';
import { sessionsApi } from '../../api/sessions.api';
import { formatSessionTime } from '../../utils/format';
import { useLookupsStore } from '../../stores/lookups';
import { useToast } from '../../composables/useToast';
import PageHeader from '../../components/ui/PageHeader.vue';
import EmptyState from '../../components/ui/EmptyState.vue';
import LoadingState from '../../components/ui/LoadingState.vue';
import ErrorState from '../../components/ui/ErrorState.vue';
import MessageModal from '../../components/ui/MessageModal.vue';
import GroupCard from '../../components/groups/GroupCard.vue';

const lookups = useLookupsStore();
const toast = useToast();

const activeTab = ref('discover');
const courseFilter = ref('');
const discover = ref([]);
const mine = ref([]);
const upcomingSessions = ref([]);
const loading = ref(true);
const error = ref('');

const groups = computed(() => (activeTab.value === 'discover' ? discover.value : mine.value));
const emptyState = computed(() =>
	activeTab.value === 'discover'
		? { icon: 'bi-collection', title: 'No open study groups', message: courseFilter.value ? 'No open groups for this course yet.' : 'Check back later, or create one yourself.' }
		: { icon: 'bi-people', title: 'You are not in any group yet', message: 'Join a group from the Discover tab, or create your own.' },
);

async function load() {
	loading.value = true;
	error.value = '';
	try {
		await lookups.loadCourses();
		// The reminder strip is a convenience: if it fails to load, the groups should still show.
		[discover.value, mine.value, upcomingSessions.value] = await Promise.all([
			groupsApi.list(courseFilter.value),
			groupsApi.mine(),
			sessionsApi.upcoming().catch(() => []),
		]);
	} catch (caught) {
		error.value = caught.message;
	} finally {
		loading.value = false;
	}
}

onMounted(load);
watch(courseFilter, load);

// ---- Request to join ------------------------------------------------------
const joinOpen = ref(false);
const joinTarget = ref(null);
const sending = ref(false);

function openJoin(group) {
	joinTarget.value = group;
	joinOpen.value = true;
}

async function sendJoinRequest(message) {
	const group = joinTarget.value;
	sending.value = true;
	try {
		await groupsApi.requestToJoin(group.id, message);
		joinOpen.value = false;
		toast.success(`Request sent to the leader of ${group.name}.`);
		await load();
	} catch (caught) {
		toast.error(caught.message);
	} finally {
		sending.value = false;
	}
}

const isFull = (group) => group.memberCount >= group.maxSize;
</script>

<template>
	<div>
		<PageHeader title="Study groups" subtitle="Join a group for a course, or start your own and lead it.">
			<template #actions>
				<RouterLink class="btn btn-primary" to="/groups/new"><i class="bi bi-plus-lg me-2"></i>Create group</RouterLink>
			</template>
		</PageHeader>

		<section v-if="upcomingSessions.length" class="card-flat px-3 py-2 mb-4" aria-label="Your next study sessions">
			<h2 class="h6 mt-1 mb-2"><i class="bi bi-calendar-event me-2"></i>Your next sessions</h2>
			<ul class="list-unstyled mb-1">
				<li v-for="session in upcomingSessions.slice(0, 3)" :key="session.id" class="small mb-1">
					<RouterLink :to="{ name: 'group-view', params: { id: session.groupId } }" class="fw-medium">{{ session.title }}</RouterLink>
					<span class="text-muted"> · {{ session.groupName }} · {{ formatSessionTime(session.startsAt, session.endsAt) }}</span>
				</li>
			</ul>
		</section>

		<div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
			<ul class="nav nav-underline" role="tablist">
				<li class="nav-item" role="presentation">
					<button type="button" class="nav-link" :class="{ active: activeTab === 'discover' }" role="tab" :aria-selected="activeTab === 'discover'" @click="activeTab = 'discover'">Discover</button>
				</li>
				<li class="nav-item" role="presentation">
					<button type="button" class="nav-link" :class="{ active: activeTab === 'mine' }" role="tab" :aria-selected="activeTab === 'mine'" @click="activeTab = 'mine'">
						My groups
						<span class="badge rounded-pill ms-1" :class="activeTab === 'mine' ? 'text-bg-primary' : 'text-bg-light border'">{{ mine.length }}</span>
					</button>
				</li>
			</ul>

			<select v-if="activeTab === 'discover'" v-model="courseFilter" class="form-select w-auto" aria-label="Filter by course">
				<option value="">All courses</option>
				<option v-for="course in lookups.courses" :key="course.code" :value="course.code">{{ course.code }} · {{ course.name }}</option>
			</select>
		</div>

		<ErrorState v-if="error" :message="error" @retry="load" />
		<LoadingState v-else-if="loading" />
		<div v-else-if="groups.length === 0" class="card-flat"><EmptyState v-bind="emptyState" /></div>
		<div v-else class="row g-3">
			<div v-for="group in groups" :key="group.id" class="col-md-6 col-xl-4">
				<GroupCard :group="group">
					<template #actions>
						<RouterLink v-if="group.myRole === 'LEADER' && group.status === 'OPEN'" class="btn btn-sm btn-primary" :to="{ name: 'group-manage', params: { id: group.id } }">
							Manage group
						</RouterLink>
						<RouterLink v-else-if="group.myRole === 'LEADER' || group.myRole === 'MEMBER'" class="btn btn-sm btn-light border" :to="{ name: group.myRole === 'LEADER' ? 'group-manage' : 'group-view', params: { id: group.id } }">
							View
						</RouterLink>
						<button v-else-if="group.status === 'CLOSED'" type="button" class="btn btn-sm btn-light border" disabled>
							Closed
						</button>
						<button v-else-if="group.myRequestStatus === 'PENDING'" type="button" class="btn btn-sm btn-light border" disabled>Request pending</button>
						<button v-else-if="isFull(group)" type="button" class="btn btn-sm btn-light border" disabled>Group full</button>
						<button v-else type="button" class="btn btn-sm btn-outline-primary" @click="openJoin(group)">Request to join</button>
					</template>
				</GroupCard>
			</div>
		</div>

		<MessageModal
			v-model="joinOpen"
			title="Request to join"
			:subtitle="joinTarget ? `Ask the leader of “${joinTarget.name}” to let you in.` : ''"
			placeholder="Tell the leader a bit about yourself (optional)"
			submit-label="Send request"
			:busy="sending"
			@submit="sendJoinRequest"
		/>
	</div>
</template>
