<script setup>
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { groupsApi } from '../../api/groups.api';
import { useLookupsStore } from '../../stores/lookups';
import { useToast } from '../../composables/useToast';
import { useConfirm } from '../../composables/useConfirm';
import { useAsync } from '../../composables/useAsync';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusBadge from '../../components/ui/StatusBadge.vue';
import LoadingState from '../../components/ui/LoadingState.vue';
import ErrorState from '../../components/ui/ErrorState.vue';
import GroupForm from '../../components/groups/GroupForm.vue';
import GroupDetails from '../../components/groups/GroupDetails.vue';
import GroupBestTimes from '../../components/groups/GroupBestTimes.vue';
import JoinRequestList from '../../components/groups/JoinRequestList.vue';
import MemberList from '../../components/groups/MemberList.vue';
import LeadershipTransferModal from '../../components/admin/LeadershipTransferModal.vue';

const props = defineProps({ id: { type: String, required: true } });

const lookups = useLookupsStore();
const router = useRouter();
const route = useRoute();
const toast = useToast();
const { confirm } = useConfirm();

const group = ref(null);
const requests = ref([]);
const activeTab = ref('requests');
const busyRequestId = ref(null);
const saving = ref(false);
const leadershipOpen = ref(false);
const isLeader = computed(() => group.value?.myRole === 'LEADER');
const viewOnly = computed(() => route.name === 'group-view');
const readonly = computed(() => viewOnly.value || !isLeader.value || group.value?.status === 'CLOSED');

const { loading, error, run: load } = useAsync(async () => {
	await lookups.loadCourses();
	const loaded = await groupsApi.get(props.id);
	if (!['LEADER', 'MEMBER'].includes(loaded.myRole)) {
		toast.error('Only group members can view this page.');
		await router.replace('/groups');
		return;
	}
	if (loaded.myRole === 'MEMBER' && (group.value?.id !== loaded.id || group.value?.myRole !== 'MEMBER')) activeTab.value = 'details';
	group.value = loaded;
	if (loaded.myRole === 'MEMBER' && route.name === 'group-manage') {
		await router.replace({ name: 'group-view', params: { id: props.id } });
	}
	requests.value = loaded.status === 'OPEN' ? await groupsApi.joinRequests(props.id) : [];
	if (loaded.status === 'CLOSED') activeTab.value = 'members';
});

watch(() => props.id, load, { immediate: true });

async function refresh() {
	await load();
}

async function decide(request, accept) {
	if (readonly.value || busyRequestId.value) return;
	busyRequestId.value = request.id;
	try {
		if (accept) await groupsApi.acceptRequest(props.id, request.id);
		else await groupsApi.rejectRequest(props.id, request.id);
		toast.success(accept ? `${request.student.name} joined the group.` : `Request from ${request.student.name} rejected.`);
		await refresh();
	} catch (caught) {
		toast.error(caught.message);
	} finally {
		busyRequestId.value = null;
	}
}

async function removeMember(member) {
	if (readonly.value) return;
	const confirmed = await confirm({
		title: `Remove ${member.name}?`,
		message: 'They will lose access to this group and will need to request to join again.',
		confirmLabel: 'Remove member',
		danger: true,
	});
	if (!confirmed) return;
	try {
		await groupsApi.removeMember(props.id, member.id);
		toast.success(`${member.name} was removed from the group.`);
		await refresh();
	} catch (caught) {
		toast.error(caught.message);
	}
}

async function saveDetails(payload) {
	if (readonly.value || saving.value) return;
	saving.value = true;
	try {
		group.value = await groupsApi.update(props.id, payload);
		toast.success('Group details updated.');
	} catch (caught) {
		toast.error(caught.message);
	} finally {
		saving.value = false;
	}
}

async function closeGroup() {
	if (readonly.value) return;
	const confirmed = await confirm({
		title: 'Close this group?',
		message: 'Pending join requests will be rejected and no one else can join. This cannot be undone.',
		confirmLabel: 'Close group',
		danger: true,
	});
	if (!confirmed) return;
	try {
		await groupsApi.close(props.id);
		await refresh();
		activeTab.value = 'members';
		toast.success('The group has been closed.');
	} catch (caught) {
		toast.error(caught.message);
	}
}
</script>

<template>
	<div>
		<RouterLink to="/groups" class="text-decoration-none small"><i class="bi bi-arrow-left me-1"></i>Study groups</RouterLink>

		<LoadingState v-if="loading" />
		<ErrorState v-else-if="error" class="mt-3" :message="error" @retry="load" />

		<template v-else-if="group">
			<PageHeader class="mt-2" :title="group.name" :subtitle="`${group.courseCode} · ${group.courseName}`">
				<template #actions>
					<StatusBadge :status="group.status" class="align-self-center" />
					<RouterLink v-if="isLeader && viewOnly" class="btn btn-primary" :to="{ name: 'group-manage', params: { id } }">Manage group</RouterLink>
					<button v-if="isLeader && !viewOnly" type="button" class="btn btn-light border" @click="leadershipOpen = true">
						<i class="bi bi-person-gear me-2"></i>Transfer leadership
					</button>
					<button v-if="!readonly" type="button" class="btn btn-outline-danger" @click="closeGroup">
						<i class="bi bi-lock me-2"></i>Close group
					</button>
				</template>
			</PageHeader>

			<div v-if="group.status === 'CLOSED'" class="alert alert-secondary" role="status">
				<i class="bi bi-lock me-2"></i>This group is closed. It is kept here as a read-only record.
			</div>

			<ul class="nav nav-underline mb-4" role="tablist">
				<li v-if="group.status === 'OPEN'" class="nav-item" role="presentation">
					<button type="button" class="nav-link" :class="{ active: activeTab === 'requests' }" role="tab" @click="activeTab = 'requests'">
						Join requests
						<span class="badge rounded-pill ms-1" :class="requests.length ? 'text-bg-primary' : 'text-bg-light border'">{{ requests.length }}</span>
					</button>
				</li>
				<li class="nav-item" role="presentation">
					<button type="button" class="nav-link" :class="{ active: activeTab === 'members' }" role="tab" @click="activeTab = 'members'">
						Members
						<span class="badge rounded-pill text-bg-light border ms-1">{{ group.memberCount }}/{{ group.maxSize }}</span>
					</button>
				</li>
				<li class="nav-item" role="presentation">
					<button type="button" class="nav-link" :class="{ active: activeTab === 'details' }" role="tab" @click="activeTab = 'details'">Details</button>
				</li>
			</ul>

			<div class="card-flat px-3 px-md-4 py-2 py-md-3">
				<JoinRequestList
					v-if="activeTab === 'requests' && group.status === 'OPEN'"
					:requests="requests"
					:busy-id="busyRequestId"
					:readonly="readonly"
					@accept="decide($event, true)"
					@reject="decide($event, false)"
				/>
				<MemberList v-else-if="activeTab === 'members'" :members="group.members" :readonly="readonly" @remove="removeMember" />
				<div v-else-if="activeTab === 'details'" class="py-3">
					<GroupDetails v-if="readonly" :group="group" />
					<GroupForm v-else :group="group" :saving="saving" submit-label="Save changes" @submit="saveDetails" />
					<GroupBestTimes class="mt-4" :group-id="id" :member-count="group.memberCount" :closed="group.status === 'CLOSED'" />
				</div>
			</div>
			<LeadershipTransferModal v-model="leadershipOpen" :group-id="id" @transferred="refresh" />
		</template>
	</div>
</template>
