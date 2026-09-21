<script setup>
import { computed, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { GROUP_FORMATS, MEETING_MODES, STUDY_GOALS, labelOf } from '../../utils/constants';
import { formatDuration } from '../../utils/availability';
import { programmeLine } from '../../utils/format';
import StudentAvatar from '../ui/StudentAvatar.vue';
import StatusBadge from '../ui/StatusBadge.vue';
import ScoreRing from '../ui/ScoreRing.vue';
import ScoreBreakdown from './ScoreBreakdown.vue';

const props = defineProps({
	/** One result from GET /api/matches. */
	match: { type: Object, required: true },
	rank: { type: Number, required: true },
});
defineEmits(['view', 'request']);

const expanded = ref(false);
const student = computed(() => props.match.student);
const showsStatus = computed(() => props.match.connectionStatus !== 'NONE');
</script>

<template>
	<article class="card-flat">
		<div class="p-3 p-md-4 d-flex gap-3 align-items-start">
			<span class="rank text-muted fw-semibold d-none d-sm-block">#{{ rank }}</span>
			<StudentAvatar :name="student.name" />

			<div class="flex-grow-1 min-w-0">
				<div class="d-flex flex-wrap align-items-center gap-2 mb-1">
					<h2 class="h6 mb-0">{{ student.name }}</h2>
					<StatusBadge v-if="showsStatus" :status="match.connectionStatus" />
				</div>
				<div class="text-muted small mb-2">{{ programmeLine(student) }}</div>

				<div class="d-flex flex-wrap gap-1 mb-2">
					<span v-for="code in match.sharedCourses" :key="code" class="chip-static"><i class="bi bi-book"></i>{{ code }}</span>
					<span v-for="goal in student.preferences.goals" :key="goal" class="chip-static">
						{{ labelOf(STUDY_GOALS, goal) }}
					</span>
				</div>

				<div class="d-flex flex-wrap column-gap-3 row-gap-1 small text-muted">
					<span><i class="bi bi-calendar-check me-1"></i>{{ match.overlapMinutes ? `${formatDuration(match.overlapMinutes)} weekly overlap` : 'No shared time slots' }}</span>
					<span><i class="bi bi-geo-alt me-1"></i>{{ labelOf(MEETING_MODES, student.preferences.meetingMode) }}</span>
					<span><i class="bi bi-people me-1"></i>{{ labelOf(GROUP_FORMATS, student.preferences.groupFormat) }}</span>
				</div>
			</div>

			<ScoreRing :value="match.score" />
		</div>

		<div v-if="expanded" class="px-3 px-md-4 pb-3">
			<div class="section-title">Why this score</div>
			<ScoreBreakdown :breakdown="match.breakdown" />
		</div>

		<footer class="d-flex flex-wrap align-items-center justify-content-between gap-2 border-top px-3 px-md-4 py-2">
			<button type="button" class="btn btn-link btn-sm text-decoration-none px-0" :aria-expanded="expanded" @click="expanded = !expanded">
				<i class="bi" :class="expanded ? 'bi-chevron-up' : 'bi-chevron-down'"></i> Why this score
			</button>
			<div class="d-flex gap-2">
				<button type="button" class="btn btn-sm btn-light border" @click="$emit('view', student.id)">View profile</button>
				<button
					v-if="match.connectionStatus === 'NONE'"
					type="button"
					class="btn btn-sm btn-primary"
					@click="$emit('request', student)"
				>
					<i class="bi bi-person-plus me-1"></i>Send request
				</button>
				<RouterLink
					v-else-if="match.connectionStatus === 'PENDING_RECEIVED'"
					class="btn btn-sm btn-primary"
					:to="{ name: 'connections', query: { tab: 'incoming' } }"
				>
					Respond
				</RouterLink>
			</div>
		</footer>
	</article>
</template>

<style scoped>
.rank {
	width: 1.75rem;
	padding-top: 0.6rem;
}
.min-w-0 {
	min-width: 0;
}
</style>
