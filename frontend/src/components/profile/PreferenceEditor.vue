<script setup>
import ChipSelect from '../ui/ChipSelect.vue';
import SegmentedControl from '../ui/SegmentedControl.vue';
import AvailabilityEditor from '../ui/AvailabilityEditor.vue';
import { GROUP_FORMATS, MEETING_MODES, STUDY_GOALS } from '../../utils/constants';

const props = defineProps({
	/** One study preference: { course, meetingMode, groupFormat, goals, availability }. */
	modelValue: { type: Object, required: true },
	/** "IS442 · Object-Oriented Application Development" */
	courseLabel: { type: String, required: true },
	/** Field errors for this preference: { goals?, availability? }. */
	errors: { type: Object, default: () => ({}) },
});
const emit = defineEmits(['update:modelValue', 'remove']);

function update(field, value) {
	emit('update:modelValue', { ...props.modelValue, [field]: value });
}
</script>

<template>
	<article class="border rounded-3 p-3 p-md-4">
		<header class="d-flex align-items-start justify-content-between gap-2 mb-3">
			<h3 class="h6 mb-0"><i class="bi bi-book me-2 text-primary"></i>{{ courseLabel }}</h3>
			<button type="button" class="btn btn-sm btn-link text-danger text-decoration-none p-0" @click="$emit('remove')">
				<i class="bi bi-trash me-1"></i>Remove
			</button>
		</header>

		<div class="row">
			<div class="col-md-6 mb-3">
				<div class="form-label">Meeting mode</div>
				<SegmentedControl :model-value="modelValue.meetingMode" :options="MEETING_MODES" @update:model-value="update('meetingMode', $event)" />
			</div>
			<div class="col-md-6 mb-3">
				<div class="form-label">Group format</div>
				<SegmentedControl :model-value="modelValue.groupFormat" :options="GROUP_FORMATS" @update:model-value="update('groupFormat', $event)" />
			</div>
		</div>

		<div class="mb-3">
			<div class="form-label">Study goals <span class="text-danger">*</span></div>
			<ChipSelect :model-value="modelValue.goals" :options="STUDY_GOALS" @update:model-value="update('goals', $event)" />
			<div v-if="errors.goals" class="invalid-feedback d-block">{{ errors.goals }}</div>
		</div>

		<div>
			<div class="form-label">Weekly availability <span class="text-danger">*</span></div>
			<AvailabilityEditor :model-value="modelValue.availability" @update:model-value="update('availability', $event)" />
			<div v-if="errors.availability" class="invalid-feedback d-block">{{ errors.availability }}</div>
		</div>
	</article>
</template>
