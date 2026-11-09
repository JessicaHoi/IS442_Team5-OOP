<script setup>
import { onBeforeUnmount, ref, watch } from 'vue';
import { adminApi } from '../../api/admin.api';
import AppModal from '../ui/AppModal.vue';
import LoadingState from '../ui/LoadingState.vue';
import ErrorState from '../ui/ErrorState.vue';
import EmptyState from '../ui/EmptyState.vue';
import StatusBadge from '../ui/StatusBadge.vue';
import MemberList from '../groups/MemberList.vue';

const props = defineProps({
	modelValue: Boolean,
	user: { type: Object, default: null },
});
const emit = defineEmits(['update:modelValue']);
const groups = ref([]);
const loading = ref(false);
const error = ref('');
let requestVersion = 0;

async function load() {
	const version = ++requestVersion;
	if (!props.modelValue || !props.user) return;
	loading.value = true;
	error.value = '';
	try {
		const loaded = await adminApi.listUserGroups(props.user.id);
		if (version === requestVersion) groups.value = loaded;
	} catch (caught) {
		if (version === requestVersion) error.value = caught.message;
	} finally {
		if (version === requestVersion) loading.value = false;
	}
}

watch(() => [props.modelValue, props.user?.id], ([open]) => {
	requestVersion++;
	groups.value = [];
	error.value = '';
	loading.value = false;
	if (open) load();
}, { immediate: true });
onBeforeUnmount(() => requestVersion++);
</script>

<template>
	<AppModal :model-value="modelValue" :title="`Groups: ${user?.name ?? ''}`" size="lg" @update:model-value="emit('update:modelValue', $event)">
		<LoadingState v-if="loading" label="Loading groups…" />
		<ErrorState v-else-if="error" :message="error" @retry="load" />
		<EmptyState v-else-if="!groups.length" icon="bi-people" title="No groups yet" message="This student is not a member of any study groups." />
		<div v-else class="d-flex flex-column gap-3">
			<section v-for="group in groups" :key="group.id" class="border rounded-3 p-3" :aria-label="group.name">
				<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-1">
					<h3 class="h6 mb-0">{{ group.name }}</h3>
					<StatusBadge :status="group.status" />
				</div>
				<p class="small text-muted mb-3">{{ group.courseCode }} · {{ group.courseName }}</p>
				<h4 class="small fw-semibold mb-1">Members <span class="text-muted fw-normal">{{ group.memberCount }}/{{ group.maxSize }}</span></h4>
				<MemberList :members="group.members" readonly show-member-roles />
			</section>
		</div>
		<template #footer><button type="button" class="btn btn-light" data-bs-dismiss="modal">Close</button></template>
	</AppModal>
</template>
