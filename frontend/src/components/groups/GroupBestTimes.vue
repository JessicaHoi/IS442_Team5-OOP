<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { groupsApi } from '../../api/groups.api';
import { formatSlot } from '../../utils/availability';
import LoadingState from '../ui/LoadingState.vue';
import ErrorState from '../ui/ErrorState.vue';
import AvailabilityGrid from './AvailabilityGrid.vue';
import GroupAvailabilityModal from './GroupAvailabilityModal.vue';

const props = defineProps({
	groupId: { type: String, required: true },
	memberCount: { type: Number, default: 0 },
	closed: Boolean,
});
const data = ref({ suggestions: [], heatmap: [], memberCount: 0 });
const loading = ref(false);
const error = ref('');
const editorOpen = ref(false);
let requestVersion = 0;

async function load() {
	const version = ++requestVersion;
	loading.value = true;
	error.value = '';
	try {
		const response = await groupsApi.bestTimes(props.groupId);
		if (version === requestVersion) data.value = response;
	} catch (caught) {
		if (version === requestVersion) error.value = caught.message;
	} finally {
		if (version === requestVersion) loading.value = false;
	}
}

watch(() => [props.groupId, props.memberCount], load, { immediate: true });
watch(() => props.closed, (closed) => { if (closed) editorOpen.value = false; });
onMounted(() => window.addEventListener('focus', load));
onBeforeUnmount(() => { requestVersion++; window.removeEventListener('focus', load); });
const windowLabel = (slot) => formatSlot({ day: slot.day.slice(0, 3), start: (slot.startTime ?? slot.start).slice(0, 5), end: (slot.endTime ?? slot.end).slice(0, 5) });
</script>

<template>
	<section aria-label="Best time to meet">
		<div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
			<div><h2 class="h5 mb-1"><i class="bi bi-calendar-heart me-2"></i>Best time to meet</h2><p class="text-body-secondary small mb-0">Each cell is 30 minutes. Darker means more members are available. Hover, focus or tap a cell to see who is available.</p></div>
			<div class="d-flex gap-2">
				<button type="button" class="btn btn-light border btn-sm" :disabled="loading" @click="load">Refresh</button>
				<button v-if="!closed" type="button" class="btn btn-primary btn-sm" @click="editorOpen = true">Update my availability</button>
			</div>
		</div>
		<p v-if="closed" class="small text-muted">This group is closed. Availability is read-only.</p>
		<LoadingState v-if="loading" />
		<ErrorState v-else-if="error" :message="error" @retry="load" />
		<template v-else>
			<div class="d-flex flex-wrap gap-2 mb-3">
				<span v-for="suggestion in data.suggestions" :key="`${suggestion.slot.day}-${suggestion.slot.startTime ?? suggestion.slot.start}`" class="badge rounded-pill text-bg-light border fw-semibold">{{ windowLabel(suggestion.slot) }} · {{ suggestion.freeCount }} of {{ data.memberCount }} free</span>
				<span v-if="!data.suggestions.length" class="text-body-secondary small">No shared free time yet. Update your availability for this group to help find a time.</span>
			</div>
			<AvailabilityGrid :cells="data.heatmap" :member-count="data.memberCount" />
		</template>
		<GroupAvailabilityModal v-model="editorOpen" :group-id="groupId" @saved="load" />
	</section>
</template>
