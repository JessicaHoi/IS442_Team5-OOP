<script setup>
import { computed } from 'vue';
import { MATCHING_CRITERIA } from '../../utils/constants';

const props = defineProps({
	/** { course, availability, studyMode, studyGoal, groupSize } each 0..1 */
	breakdown: { type: Object, required: true },
});

const rows = computed(() =>
	MATCHING_CRITERIA.filter((criterion) => criterion.key in props.breakdown).map((criterion) => ({
		...criterion,
		percent: Math.round(props.breakdown[criterion.key] * 100),
	})),
);
</script>

<template>
	<ul class="list-unstyled mb-0">
		<li v-for="row in rows" :key="row.key" class="d-flex align-items-center gap-3 mb-2">
			<span class="small text-muted label">{{ row.label }}</span>
			<div class="progress flex-grow-1" style="height: 6px" role="progressbar" :aria-valuenow="row.percent" aria-valuemin="0" aria-valuemax="100">
				<div class="progress-bar" :style="{ width: `${row.percent}%` }"></div>
			</div>
			<span class="small fw-medium value">{{ row.percent }}%</span>
		</li>
	</ul>
</template>

<style scoped>
.label {
	width: 9.5rem;
	flex-shrink: 0;
}
.value {
	width: 2.5rem;
	text-align: right;
}
</style>
