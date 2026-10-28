<script setup>
import { onBeforeUnmount, ref, watch } from 'vue';
import { adminApi } from '../../api/admin.api';
import { groupsApi } from '../../api/groups.api';
import { useToast } from '../../composables/useToast';
import AppModal from '../ui/AppModal.vue';
import LoadingState from '../ui/LoadingState.vue';
import ErrorState from '../ui/ErrorState.vue';
import EmptyState from '../ui/EmptyState.vue';
import FormField from '../ui/FormField.vue';
import StatusBadge from '../ui/StatusBadge.vue';

const props = defineProps({
	modelValue: Boolean,
	user: { type: Object, default: null },
	groupId: { type: String, default: null },
});
const emit = defineEmits(['update:modelValue', 'transferred']);
const toast = useToast();
const groups = ref([]);
const selected = ref({});
const loading = ref(false);
const saving = ref(false);
const error = ref('');
let requestVersion = 0;

async function load() {
	const version = ++requestVersion;
	const userId = props.user?.id;
	if ((!userId && !props.groupId) || !props.modelValue) return;
	loading.value = true;
	error.value = '';
	try {
		const loaded = props.groupId ? [await groupsApi.leadership(props.groupId)] : await adminApi.listLedGroups(userId);
		if (version === requestVersion) {
			groups.value = loaded;
			for (const group of loaded) {
				if (!group.eligibleMembers.some((member) => member.id === selected.value[group.id])) selected.value[group.id] = undefined;
			}
		}
	} catch (caught) {
		if (version === requestVersion) error.value = caught.message;
	} finally {
		if (version === requestVersion) loading.value = false;
	}
}

watch(() => [props.modelValue, props.user?.id, props.groupId], ([open]) => {
	requestVersion++;
	loading.value = false;
	if (open && (props.user || props.groupId)) {
		selected.value = {};
		groups.value = [];
		load();
	}
});

onBeforeUnmount(() => requestVersion++);

async function transfer(group) {
	if (saving.value || loading.value || !group.eligibleMembers.some((member) => member.id === selected.value[group.id])) return;
	const userId = props.user?.id;
	const groupId = props.groupId;
	saving.value = true;
	error.value = '';
	try {
		if (groupId) await groupsApi.transferLeadership(groupId, selected.value[group.id]);
		else await adminApi.transferLeadership(userId, group.id, selected.value[group.id]);
		toast.success('Group leadership transferred.');
		emit('transferred');
		if (groupId) emit('update:modelValue', false);
		else if (props.modelValue && props.user?.id === userId) await load();
	} catch (caught) {
		if (props.modelValue && props.user?.id === userId && props.groupId === groupId) error.value = caught.message;
	} finally {
		saving.value = false;
	}
}
</script>

<template>
	<AppModal :model-value="modelValue" :busy="saving" :title="groupId ? 'Transfer group leadership' : `Transfer leadership: ${user?.name ?? ''}`" @update:model-value="emit('update:modelValue', $event)">
		<LoadingState v-if="loading" label="Loading groups…" />
		<ErrorState v-else-if="error" :message="error" @retry="load" />
		<EmptyState v-else-if="!groups.length" title="This student does not lead any groups." />
		<template v-else>
			<div v-for="group in groups" :key="group.id" class="mb-4">
				<div class="d-flex align-items-center gap-2 mb-2"><span class="fw-medium">{{ group.name }}</span><StatusBadge :status="group.status" /></div>
				<FormField :label="`New leader for ${group.name}`" :hint="!group.eligibleMembers.length ? (groupId ? 'There are no other active members to appoint.' : 'There are no other active members. Account deletion remains blocked.') : ''" v-slot="{ id }">
					<select :id="id" v-model="selected[group.id]" class="form-select" :disabled="saving || !group.eligibleMembers.length">
						<option :value="undefined" disabled>Choose an active member</option>
						<option v-for="member in group.eligibleMembers" :key="member.id" :value="member.id">{{ member.name }}</option>
					</select>
				</FormField>
				<button type="button" class="btn btn-primary btn-sm" :disabled="saving || !selected[group.id]" @click="transfer(group)">
					<span v-if="saving" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Transfer leadership
				</button>
			</div>
		</template>
		<template #footer><button type="button" class="btn btn-light" data-bs-dismiss="modal" :disabled="saving">Close</button></template>
	</AppModal>
</template>
