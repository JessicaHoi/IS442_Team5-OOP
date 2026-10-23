<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { groupsApi } from '../../api/groups.api';
import { useAsync } from '../../composables/useAsync';
import LoadingState from '../ui/LoadingState.vue';
import ErrorState from '../ui/ErrorState.vue';

const props = defineProps({
	groupId: { type: String, required: true },
	memberCount: { type: Number, default: 0 },
});

// Day codes match the rest of the app (WED, not WEDNESDAY).
const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
const SHORT = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const FIRST_HOUR = 7; // must match SchedulingService
const LAST_HOUR = 23;
const HOURS = Array.from({ length: LAST_HOUR - FIRST_HOUR }, (_, i) => FIRST_HOUR + i);

const data = ref({ suggestions: [], heatmap: [] });

const { loading, error, run: load } = useAsync(async () => {
	data.value = await groupsApi.bestTimes(props.groupId);
});

onMounted(load);
// Re-check when someone joins or is removed.
watch(() => props.memberCount, load);

// Accepts "WEDNESDAY" or "WED", and "19:00:00" or "19:00", so it works
// whichever shape the API sends.
const dayCode = (day) => day.slice(0, 3).toUpperCase();
const hourOf = (time) => Number(time.slice(0, 2));
const startOf = (slot) => slot.startTime ?? slot.start;
const endOf = (slot) => slot.endTime ?? slot.end;
const label = (hour) => `${String(hour).padStart(2, '0')}:00`;

const counts = computed(() => new Map(data.value.heatmap.map((c) => [`${dayCode(c.day)}-${c.hour}`, c.freeCount])));
const max = computed(() => props.memberCount || Math.max(1, ...data.value.heatmap.map((c) => c.freeCount)));

const bestKeys = computed(() => {
	const keys = new Set();
	for (const w of data.value.suggestions) {
		for (let h = hourOf(startOf(w.slot)); h < hourOf(endOf(w.slot)); h++) keys.add(`${dayCode(w.slot.day)}-${h}`);
	}
	return keys;
});

const freeAt = (day, hour) => counts.value.get(`${day}-${hour}`) || 0;
const isBest = (day, hour) => bestKeys.value.has(`${day}-${hour}`);

function cellStyle(day, hour) {
	const n = freeAt(day, hour);
	return n > 0 ? { backgroundColor: `rgba(var(--bs-primary-rgb), ${0.15 + 0.85 * (n / max.value)})` } : {};
}

function chipText(w) {
	const day = SHORT[DAYS.indexOf(dayCode(w.slot.day))];
	return `${day} ${label(hourOf(startOf(w.slot)))}-${label(hourOf(endOf(w.slot)))} · ${w.freeCount} of ${max.value} free`;
}
</script>

<template>
	<section aria-labelledby="best-times-title">
		<h2 id="best-times-title" class="h5 mb-1"><i class="bi bi-calendar-heart me-2"></i>Best time to meet</h2>
		<p class="text-body-secondary small mb-3">
			Based on each member's availability for this course. Darker means more people are free; outlined cells are the suggested windows.
		</p>

		<LoadingState v-if="loading" />
		<ErrorState v-else-if="error" :message="error" @retry="load" />

		<template v-else>
			<div class="d-flex flex-wrap gap-2 mb-3">
				<span v-for="w in data.suggestions" :key="`${w.slot.day}-${startOf(w.slot)}`" class="badge rounded-pill text-bg-light border border-warning border-2 fw-semibold">
					{{ chipText(w) }}
				</span>
				<span v-if="!data.suggestions.length" class="text-body-secondary small">
					No shared free time yet. Members need to set their availability for this course.
				</span>
			</div>

			<div class="table-responsive">
				<div class="heatmap" role="img" aria-label="Weekly availability heatmap for the group">
					<div></div>
					<div v-for="d in SHORT" :key="d" class="axis">{{ d }}</div>
					<template v-for="hour in HOURS" :key="hour">
						<div class="axis">{{ label(hour) }}</div>
						<div
							v-for="(day, i) in DAYS"
							:key="`${day}-${hour}`"
							class="cell"
							:class="{ best: isBest(day, hour) }"
							:style="cellStyle(day, hour)"
							:title="`${SHORT[i]} ${label(hour)}: ${freeAt(day, hour)} free`"
						></div>
					</template>
				</div>
			</div>
		</template>
	</section>
</template>

<style scoped>
.heatmap {
	display: grid;
	grid-template-columns: 3rem repeat(7, minmax(2.5rem, 1fr));
	gap: 3px;
	min-width: 420px;
}
.axis {
	font-size: 0.75rem;
	color: var(--bs-secondary-color);
	text-align: center;
	align-self: center;
}
.cell {
	height: 1.6rem;
	border-radius: 4px;
	background-color: var(--bs-tertiary-bg);
}
.cell.best {
	outline: 2px solid var(--bs-warning);
	outline-offset: -2px;
}
</style>