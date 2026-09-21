<script setup>
import { onMounted, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
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
import JoinRequestList from '../../components/groups/JoinRequestList.vue';
import MemberList from '../../components/groups/MemberList.vue';

const props = defineProps({ id: { type: Number, required: true } });

const lookups = useLookupsStore();
const router = useRouter();
const toast = useToast();
const { confirm } = useConfirm();

const group = ref(null);
const requests = ref([]);
const activeTab = ref('requests');
const busyRequestId = ref(null);
const saving = ref(false);

const { loading, error, run: load } = useAsync(async () => {
	await lookups.loadCourses();
	const loaded = await groupsApi.get(props.id);
	if (loaded.myRole !== 'LEADER') {
		toast.error('Only the group leader can manage this group.');
		await router.replace('/groups');
		return;
	}
	group.value = loaded;
	requests.value = loaded.status === 'OPEN' ? await groupsApi.joinRequests(props.id) : [];
	if (loaded.status === 'CLOSED') activeTab.value = 'members';
});

onMounted(load);

async function refresh() {
	group.value = await groupsApi.get(props.id);
	requests.value = group.value.status === 'OPEN' ? await groupsApi.joinRequests(props.id) : [];
}

async function decide(request, accept) {
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
					<button v-if="group.status === 'OPEN'" type="button" class="btn btn-outline-danger" @click="closeGroup">
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
					@accept="decide($event, true)"
					@reject="decide($event, false)"
				/>
				<MemberList v-else-if="activeTab === 'members'" :members="group.members" :readonly="group.status === 'CLOSED'" @remove="removeMember" />
				<div v-else-if="activeTab === 'details'" class="py-3">
					<GroupForm :group="group" :saving="saving" :readonly="group.status === 'CLOSED'" submit-label="Save changes" @submit="saveDetails" />
				</div>
			</div>
		</template>
	</div>
</template>
