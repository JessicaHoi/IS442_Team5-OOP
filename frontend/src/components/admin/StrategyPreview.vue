<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { adminApi } from '../../api/admin.api';
import { useAsync } from '../../composables/useAsync';
import { MATCHING_STRATEGIES, labelOf } from '../../utils/constants';
import { formatDuration } from '../../utils/availability';
import StudentAvatar from '../ui/StudentAvatar.vue';

/**
 * Runs one sample student through the saved matching settings and through the
 * settings on the page (not saved yet), and shows both rankings side by side.
 */
const props = defineProps({
	/** The settings currently on the page: { strategy, criteria }. */
	config: { type: Object, required: true },
	/** Whether the settings on the page differ from the saved ones. */
	isDirty: { type: Boolean, default: false },
	/** False when the settings on the page are invalid (nothing enabled). */
	isValid: { type: Boolean, default: true },
});

const REFRESH_DELAY_MS = 400;

const students = ref([]);
const studentId = ref('');
const course = ref('');
const result = ref(null);

const { error: studentsError, run: loadStudents } = useAsync(async () => {
	students.value = await adminApi.listPreviewStudents();
	if (!studentId.value && students.value.length) studentId.value = students.value[0].id;
});
const { loading, error, run: runPreview } = useAsync(async () => {
	result.value = await adminApi.previewMatching(studentId.value, course.value || null, props.config);
});
onMounted(loadStudents);

const selectedStudent = computed(() => students.value.find((s) => s.id === studentId.value));
watch(studentId, () => {
	course.value = selectedStudent.value?.courses[0] ?? '';
});

// Once a preview is showing, keep it in step with the sliders and the strategy.
let refreshTimer = null;
watch(
	() => [JSON.stringify(props.config), studentId.value, course.value],
	() => {
		if (!result.value || !props.isValid) return;
		clearTimeout(refreshTimer);
		refreshTimer = setTimeout(runPreview, REFRESH_DELAY_MS);
	},
);
onBeforeUnmount(() => clearTimeout(refreshTimer));

const strategyLabel = (value) => labelOf(MATCHING_STRATEGIES, value);

/** How far a student moved compared with the saved ranking. */
function movement(entry) {
	if (entry.otherRank == null) return { text: 'new', tone: 'primary', label: 'Not in the saved top list' };
	const change = entry.otherRank - entry.rank;
	if (change > 0) return { text: `▲ ${change}`, tone: 'success', label: `Up ${change} from #${entry.otherRank}` };
	if (change < 0) return { text: `▼ ${-change}`, tone: 'danger', label: `Down ${-change} from #${entry.otherRank}` };
	return { text: '–', tone: 'secondary', label: 'Same position' };
}

const movedCount = computed(() =>
	result.value ? result.value.proposed.results.filter((entry) => entry.otherRank !== entry.rank).length : 0,
);
</script>

<template>
	<section class="card-flat p-3 p-md-4 mb-4">
		<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-1">
			<div class="section-title mb-0"><i class="bi bi-layout-split me-2"></i>Preview before saving</div>
			<span class="small text-muted">Nothing is saved by previewing.</span>
		</div>
		<p class="small text-muted mb-3">
			Pick a sample student to see their ranked matches under the saved settings and under the settings above, side by side.
		</p>

		<div v-if="studentsError" class="alert alert-danger py-2" role="alert">{{ studentsError }}</div>
		<form class="row g-2 align-items-end mb-3" @submit.prevent="runPreview">
			<div class="col-sm-6 col-lg-5">
				<label class="form-label small mb-1" for="preview-student">Sample student</label>
				<select id="preview-student" v-model="studentId" class="form-select">
					<option v-for="student in students" :key="student.id" :value="student.id">{{ student.name }}</option>
				</select>
			</div>
			<div class="col-sm-6 col-lg-3">
				<label class="form-label small mb-1" for="preview-course">Course</label>
				<select id="preview-course" v-model="course" class="form-select" :disabled="!selectedStudent">
					<option v-for="code in selectedStudent?.courses ?? []" :key="code" :value="code">{{ code }}</option>
				</select>
			</div>
			<div class="col-lg-4 d-grid d-lg-block">
				<button type="submit" class="btn btn-outline-primary" :disabled="!studentId || !isValid || loading">
					<span v-if="loading" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
					<i v-else class="bi bi-play-fill me-1"></i>Run preview
				</button>
			</div>
		</form>

		<div v-if="error" class="alert alert-danger py-2 mb-0" role="alert">{{ error }}</div>
		<template v-else-if="result">
			<p class="small mb-3" aria-live="polite">
				<strong>{{ result.studentName }}</strong><span v-if="result.course"> · {{ result.course }}</span> —
				<span v-if="!isDirty" class="text-muted">the settings above are the same as the saved ones.</span>
				<span v-else>{{ movedCount }} of the top {{ result.proposed.results.length }} change position.</span>
			</p>
			<div class="row g-3" :class="{ refreshing: loading }">
				<div v-for="side in ['current', 'proposed']" :key="side" class="col-lg-6">
					<div class="preview-column h-100 p-3" :class="{ proposed: side === 'proposed' }">
						<div class="d-flex justify-content-between align-items-center mb-2">
							<span class="fw-semibold">{{ side === 'current' ? 'Saved settings' : 'Settings above' }}</span>
							<span class="badge badge-soft-primary">{{ strategyLabel(result[side].strategy) }}</span>
						</div>
						<p v-if="!result[side].results.length" class="small text-muted mb-0">No matches for this student.</p>
						<ol class="list-unstyled mb-0">
							<li v-for="entry in result[side].results" :key="entry.studentId" class="d-flex align-items-center gap-2 py-2 border-top">
								<span class="rank text-muted small">#{{ entry.rank }}</span>
								<StudentAvatar :name="entry.name" :size="30" />
								<div class="flex-grow-1 min-w-0">
									<div class="text-truncate fw-medium">{{ entry.name }}</div>
									<div class="small text-muted">{{ formatDuration(entry.overlapMinutes) }} overlap</div>
								</div>
								<span
									v-if="side === 'proposed'"
									class="badge move"
									:class="`badge-soft-${movement(entry).tone}`"
									:title="movement(entry).label"
									:aria-label="movement(entry).label"
								>{{ movement(entry).text }}</span>
								<span class="score fw-semibold" :aria-label="`Score ${entry.score}`">{{ entry.score }}</span>
							</li>
						</ol>
					</div>
				</div>
			</div>
		</template>
	</section>
</template>

<style scoped>
.preview-column {
	border: 1px solid var(--bs-border-color);
	border-radius: 0.75rem;
}
.preview-column.proposed {
	border-color: rgba(79, 70, 229, 0.35);
	background: rgba(79, 70, 229, 0.03);
}
.rank {
	width: 2rem;
	flex-shrink: 0;
}
.score {
	width: 2.25rem;
	text-align: right;
	flex-shrink: 0;
}
.move {
	min-width: 2.5rem;
}
.min-w-0 {
	min-width: 0;
}
.refreshing {
	opacity: 0.6;
	transition: opacity 0.15s;
}
</style>
