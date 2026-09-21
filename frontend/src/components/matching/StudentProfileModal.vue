<script setup>
import AppModal from '../ui/AppModal.vue';
import StudentAvatar from '../ui/StudentAvatar.vue';
import ContactField from '../ui/ContactField.vue';
import AvailabilitySummary from '../ui/AvailabilitySummary.vue';
import LoadingState from '../ui/LoadingState.vue';
import StatusBadge from '../ui/StatusBadge.vue';
import { GROUP_FORMATS, MEETING_MODES, STUDY_GOALS, labelOf } from '../../utils/constants';
import { programmeLine } from '../../utils/format';

defineProps({
	modelValue: Boolean,
	/** Public profile from GET /api/students/{id}; null while loading. */
	student: { type: Object, default: null },
	loading: Boolean,
	error: { type: String, default: '' },
});
defineEmits(['update:modelValue', 'request']);
</script>

<template>
	<AppModal :model-value="modelValue" title="Study profile" size="lg" @update:model-value="$emit('update:modelValue', $event)">
		<LoadingState v-if="loading" />
		<div v-else-if="error" class="alert alert-danger mb-0">{{ error }}</div>
		<div v-else-if="student">
			<div class="d-flex align-items-center gap-3 mb-4">
				<StudentAvatar :name="student.name" :size="56" />
				<div>
					<div class="d-flex flex-wrap align-items-center gap-2">
						<h3 class="h5 mb-0">{{ student.name }}</h3>
						<StatusBadge v-if="student.connectionStatus !== 'NONE'" :status="student.connectionStatus" />
					</div>
					<div class="text-muted">{{ programmeLine(student) }}</div>
					<div class="text-muted small">{{ student.school }}</div>
				</div>
			</div>

			<dl class="row mb-0 gy-3">
				<dt class="col-sm-4 section-title mb-0 pt-1">Contact</dt>
				<dd class="col-sm-8 mb-0"><ContactField :value="student.contactNumber" /></dd>

				<dt class="col-sm-4 section-title mb-0 pt-1">Courses</dt>
				<dd class="col-sm-8 mb-0 d-flex flex-wrap gap-1">
					<span v-for="code in student.courses" :key="code" class="chip-static">{{ code }}</span>
				</dd>

				<dt class="col-sm-4 section-title mb-0 pt-1">Looking for a buddy in</dt>
				<dd class="col-sm-8 mb-0">{{ student.preferences.course ?? 'Not set' }}</dd>

				<dt class="col-sm-4 section-title mb-0 pt-1">Study goals</dt>
				<dd class="col-sm-8 mb-0 d-flex flex-wrap gap-1">
					<span v-for="goal in student.preferences.goals" :key="goal" class="chip-static">{{ labelOf(STUDY_GOALS, goal) }}</span>
				</dd>

				<dt class="col-sm-4 section-title mb-0 pt-1">Meeting mode</dt>
				<dd class="col-sm-8 mb-0">{{ labelOf(MEETING_MODES, student.preferences.meetingMode) }}</dd>

				<dt class="col-sm-4 section-title mb-0 pt-1">Group format</dt>
				<dd class="col-sm-8 mb-0">{{ labelOf(GROUP_FORMATS, student.preferences.groupFormat) }}</dd>

				<dt class="col-sm-4 section-title mb-0 pt-1">Weekly availability</dt>
				<dd class="col-sm-8 mb-0"><AvailabilitySummary :slots="student.preferences.availability" /></dd>
			</dl>
		</div>

		<template #footer>
			<button type="button" class="btn btn-light" data-bs-dismiss="modal">Close</button>
			<button
				v-if="student && student.connectionStatus === 'NONE'"
				type="button"
				class="btn btn-primary"
				@click="$emit('request', student)"
			>
				<i class="bi bi-person-plus me-1"></i>Send request
			</button>
		</template>
	</AppModal>
</template>
