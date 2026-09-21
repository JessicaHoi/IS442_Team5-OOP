<script setup>
import { computed, reactive, ref, watch } from 'vue';
import FormField from '../ui/FormField.vue';
import ChipSelect from '../ui/ChipSelect.vue';
import SegmentedControl from '../ui/SegmentedControl.vue';
import AvailabilityEditor from '../ui/AvailabilityEditor.vue';
import { MAX_GROUP_SIZE, MEETING_MODES, MIN_GROUP_SIZE, STUDY_GOALS } from '../../utils/constants';
import { validateSlots } from '../../utils/availability';
import { useLookupsStore } from '../../stores/lookups';

const props = defineProps({
	/** Existing group when editing; null when creating. */
	group: { type: Object, default: null },
	saving: Boolean,
	readonly: Boolean,
	submitLabel: { type: String, default: 'Save group' },
});
const emit = defineEmits(['submit']);

const lookups = useLookupsStore();
const isEditing = computed(() => props.group !== null);
const minimumSize = computed(() => Math.max(MIN_GROUP_SIZE, props.group?.memberCount ?? 1));

function initialForm() {
	return {
		courseCode: props.group?.courseCode ?? '',
		name: props.group?.name ?? '',
		description: props.group?.description ?? '',
		goals: [...(props.group?.goals ?? [])],
		meetingMode: props.group?.meetingMode ?? 'EITHER',
		availability: (props.group?.availability ?? [{ day: 'WED', start: '19:00', end: '21:00' }]).map((slot) => ({ ...slot })),
		maxSize: props.group?.maxSize ?? 5,
	};
}

const form = reactive(initialForm());
const errors = ref({});
const submitted = ref(false);

watch(() => props.group, () => Object.assign(form, initialForm()));

function validate() {
	const found = {};
	if (!form.courseCode) found.courseCode = 'Choose a course.';
	if (!form.name.trim()) found.name = 'Give your group a name.';
	if (!form.description.trim()) found.description = 'Add a short description.';
	if (form.goals.length === 0) found.goals = 'Choose at least one study goal.';
	if (form.availability.length === 0 || validateSlots(form.availability).some(Boolean)) {
		found.availability = 'Add at least one valid time slot.';
	}
	const size = Number(form.maxSize);
	if (!Number.isInteger(size) || size < minimumSize.value || size > MAX_GROUP_SIZE) {
		found.maxSize = `Enter a whole number from ${minimumSize.value} to ${MAX_GROUP_SIZE}.`;
	}
	errors.value = found;
	return Object.keys(found).length === 0;
}

// Once the user has tried to submit, keep errors in sync as they fix them.
watch(form, () => submitted.value && validate());

function onSubmit() {
	submitted.value = true;
	if (validate()) emit('submit', { ...form, maxSize: Number(form.maxSize) });
}
</script>

<template>
	<form novalidate @submit.prevent="onSubmit">
		<fieldset :disabled="readonly" class="border-0 p-0 m-0">
			<div class="row">
				<div class="col-md-6">
					<FormField label="Course" required :error="errors.courseCode" v-slot="{ id, invalidClass }">
						<select :id="id" v-model="form.courseCode" class="form-select" :class="invalidClass" :disabled="isEditing">
							<option value="" disabled>Select a course</option>
							<option v-for="course in lookups.courses" :key="course.code" :value="course.code">
								{{ course.code }} · {{ course.name }}
							</option>
						</select>
					</FormField>
				</div>
				<div class="col-md-6">
					<FormField label="Group name" required :error="errors.name" v-slot="{ id, invalidClass }">
						<input :id="id" v-model="form.name" class="form-control" :class="invalidClass" maxlength="60" />
					</FormField>
				</div>
			</div>

			<FormField label="Description" required :error="errors.description" v-slot="{ id, invalidClass }">
				<textarea :id="id" v-model="form.description" class="form-control" :class="invalidClass" rows="3" maxlength="300"></textarea>
			</FormField>

			<div class="mb-3">
				<div class="form-label">Study goals <span class="text-danger">*</span></div>
				<ChipSelect v-model="form.goals" :options="STUDY_GOALS" />
				<div v-if="errors.goals" class="invalid-feedback d-block">{{ errors.goals }}</div>
			</div>

			<div class="row">
				<div class="col-md-8">
					<div class="mb-3">
						<div class="form-label">Meeting mode</div>
						<SegmentedControl v-model="form.meetingMode" :options="MEETING_MODES" />
					</div>
				</div>
				<div class="col-md-4">
					<FormField
						label="Maximum group size"
						required
						:error="errors.maxSize"
						:hint="isEditing ? `Cannot be lower than the current ${group.memberCount} members.` : ''"
						v-slot="{ id, invalidClass }"
					>
						<input :id="id" v-model="form.maxSize" type="number" class="form-control" :class="invalidClass" :min="minimumSize" :max="MAX_GROUP_SIZE" />
					</FormField>
				</div>
			</div>

			<div class="mb-4">
				<div class="form-label">Weekly availability <span class="text-danger">*</span></div>
				<AvailabilityEditor v-model="form.availability" />
				<div v-if="errors.availability" class="invalid-feedback d-block">{{ errors.availability }}</div>
			</div>
		</fieldset>

		<button v-if="!readonly" type="submit" class="btn btn-primary" :disabled="saving">
			<span v-if="saving" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>{{ submitLabel }}
		</button>
	</form>
</template>
