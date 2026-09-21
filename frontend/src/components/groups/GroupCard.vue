<script setup>
import { computed } from 'vue';
import { MEETING_MODES, STUDY_GOALS, labelOf } from '../../utils/constants';
import StatusBadge from '../ui/StatusBadge.vue';
import AvailabilitySummary from '../ui/AvailabilitySummary.vue';

const props = defineProps({
	/** Group summary from GET /api/groups. */
	group: { type: Object, required: true },
});

const isFull = computed(() => props.group.memberCount >= props.group.maxSize);
</script>

<template>
	<article class="card-flat h-100 d-flex flex-column">
		<div class="p-3 p-md-4 flex-grow-1">
			<div class="d-flex flex-wrap align-items-center gap-2 mb-2">
				<span class="chip-static fw-medium">{{ group.courseCode }}</span>
				<StatusBadge v-if="group.status === 'CLOSED'" status="CLOSED" />
				<StatusBadge v-if="group.myRole !== 'NONE'" :status="group.myRole" />
				<StatusBadge v-else-if="group.myRequestStatus === 'PENDING'" status="PENDING" />
			</div>

			<h2 class="h6 mb-1">{{ group.name }}</h2>
			<p class="text-muted small mb-3 description">{{ group.description }}</p>

			<div class="d-flex flex-wrap gap-1 mb-3">
				<span v-for="goal in group.goals" :key="goal" class="chip-static">{{ labelOf(STUDY_GOALS, goal) }}</span>
			</div>

			<div class="d-flex flex-wrap column-gap-3 row-gap-1 small text-muted mb-3">
				<span :class="{ 'text-danger': isFull && group.status === 'OPEN' }">
					<i class="bi bi-people me-1"></i>{{ group.memberCount }}/{{ group.maxSize }} members
				</span>
				<span><i class="bi bi-geo-alt me-1"></i>{{ labelOf(MEETING_MODES, group.meetingMode) }}</span>
				<span><i class="bi bi-person-badge me-1"></i>{{ group.leader.name }}</span>
			</div>

			<AvailabilitySummary :slots="group.availability" />
		</div>

		<footer v-if="$slots.actions" class="border-top px-3 px-md-4 py-2 d-flex justify-content-end gap-2">
			<slot name="actions" />
		</footer>
	</article>
</template>

<style scoped>
.description {
	display: -webkit-box;
	-webkit-line-clamp: 2;
	line-clamp: 2;
	-webkit-box-orient: vertical;
	overflow: hidden;
}
</style>
