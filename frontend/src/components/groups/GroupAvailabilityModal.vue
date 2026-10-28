<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { groupsApi } from '../../api/groups.api';
import { useToast } from '../../composables/useToast';
import { validateSlots } from '../../utils/availability';
import { mergeAvailability } from '../../utils/groupAvailability';
import AppModal from '../ui/AppModal.vue';
import LoadingState from '../ui/LoadingState.vue';
import ErrorState from '../ui/ErrorState.vue';
import AvailabilitySummary from '../ui/AvailabilitySummary.vue';
import AvailabilityEditor from '../ui/AvailabilityEditor.vue';
import AvailabilityGrid from './AvailabilityGrid.vue';

const props = defineProps({ modelValue: Boolean, groupId: { type: String, required: true } });
const emit = defineEmits(['update:modelValue', 'saved']);
const toast = useToast();
const draft = ref([]);
const source = ref('PROFILE');
const revision = ref(null);
const loading = ref(false);
const saving = ref(false);
const loadError = ref('');
const saveError = ref('');
const conflict = ref(false);
const invalid = computed(() => validateSlots(draft.value).some(Boolean));
let requestVersion = 0;

async function load() {
	const version = ++requestVersion;
	loading.value = true;
	loadError.value = '';
	saveError.value = '';
	conflict.value = false;
	try {
		const response = await groupsApi.myAvailability(props.groupId);
		if (version !== requestVersion) return;
		draft.value = response.availability.map((slot) => ({ ...slot }));
		source.value = response.source;
		revision.value = response.revision;
	} catch (caught) {
		if (version === requestVersion) loadError.value = caught.message;
	} finally {
		if (version === requestVersion) loading.value = false;
	}
}

watch(() => [props.modelValue, props.groupId], ([open]) => {
	requestVersion++;
	if (open) load();
}, { immediate: true });
onBeforeUnmount(() => requestVersion++);

async function save(reset = false) {
	if (saving.value || loading.value || loadError.value || conflict.value) return;
	if (!reset && invalid.value) { saveError.value = 'Fix the time slots before saving.'; return; }
	saving.value = true;
	saveError.value = '';
	try {
		if (reset) await groupsApi.resetAvailability(props.groupId, revision.value);
		else await groupsApi.saveAvailability(props.groupId, { revision: revision.value, availability: mergeAvailability(draft.value) });
		toast.success(reset ? 'This group now uses your profile availability.' : 'Your availability for this group was saved.');
		emit('update:modelValue', false);
		emit('saved');
	} catch (caught) {
		saveError.value = caught.message;
		conflict.value = caught.status === 409;
	} finally {
		saving.value = false;
	}
}
</script>

<template>
	<AppModal :model-value="modelValue" title="Update my group availability" size="lg" as-form :busy="saving" @update:model-value="emit('update:modelValue', $event)" @submit="save()">
		<LoadingState v-if="loading" label="Loading your availability…" />
		<ErrorState v-else-if="loadError" :message="loadError" @retry="load" />
		<template v-else>
			<p class="small text-muted mb-2">These times apply only to this group. Your profile preferences will stay the same.</p>
			<p class="small mb-2">{{ source === 'PROFILE' ? 'Starting from your profile availability for this course.' : 'Using your saved availability for this group.' }}</p>
			<p class="small">Drag in any direction to mark a rectangle. Start on an available cell to erase. Use arrow keys to move and Space or Enter to toggle a cell.</p>
			<div v-if="saveError" class="alert alert-danger" role="alert">{{ saveError }} <button v-if="conflict" type="button" class="btn btn-sm btn-outline-danger ms-2" @click="load">Reload saved availability</button></div>
			<div class="d-flex justify-content-between align-items-center gap-2 mb-2">
				<h3 class="h6 mb-0">Your selected times</h3>
				<button type="button" class="btn btn-sm btn-light border" :disabled="saving || conflict" @click="draft = []">Clear all</button>
			</div>
			<AvailabilitySummary :slots="draft" empty-text="Unavailable for this group. Save to confirm this selection." />
			<AvailabilityGrid v-model="draft" class="editor-grid mt-3" editable :disabled="saving || invalid || conflict" />
			<details class="mt-3">
				<summary class="small mb-2">Enter exact times or availability outside 07:00–23:00</summary>
				<AvailabilityEditor v-model="draft" :disabled="saving || conflict" />
			</details>
		</template>
		<template #footer>
			<button type="button" class="btn btn-outline-secondary me-auto" :disabled="saving || loading || !!loadError || source !== 'GROUP' || conflict" @click="save(true)">Use profile availability</button>
			<button type="button" class="btn btn-light" data-bs-dismiss="modal" :disabled="saving">Cancel</button>
			<button type="submit" class="btn btn-primary" :disabled="saving || loading || !!loadError || invalid || conflict"><span v-if="saving" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Save availability</button>
		</template>
	</AppModal>
</template>

<style scoped>
.editor-grid :deep(.grid-scroll) { max-height: min(40vh, 20rem); }
</style>
