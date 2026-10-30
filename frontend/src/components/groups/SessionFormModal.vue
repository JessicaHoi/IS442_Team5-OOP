<script setup>
import { computed, ref, watch } from 'vue';
import { SESSION_MEETING_MODES } from '../../utils/constants';
import { toLocalInput } from '../../utils/calendar';
import AppModal from '../ui/AppModal.vue';
import FormField from '../ui/FormField.vue';

/** Form for scheduling or changing a study session. `session` edits it; `defaults` pre-fills a new one. */
const props = defineProps({
	modelValue: Boolean,
	session: { type: Object, default: null },
	defaults: { type: Object, default: null },
	saving: Boolean,
});
const emit = defineEmits(['update:modelValue', 'submit']);

const draft = ref({});
const submitted = ref(false);

watch(() => props.modelValue, (open) => {
	if (!open) return;
	submitted.value = false;
	draft.value = props.session
		? {
			title: props.session.title,
			startsAt: toLocalInput(props.session.startsAt),
			endsAt: toLocalInput(props.session.endsAt),
			meetingMode: props.session.meetingMode,
			location: props.session.location ?? '',
			note: props.session.note ?? '',
		}
		: { title: '', startsAt: '', endsAt: '', meetingMode: 'IN_PERSON', location: '', note: '', ...props.defaults };
}, { immediate: true });

const errors = computed(() => {
	if (!submitted.value) return {};
	const { title, startsAt, endsAt } = draft.value;
	return {
		title: title?.trim() ? '' : 'Give the session a title.',
		startsAt: startsAt ? '' : 'Choose a start time.',
		endsAt: !endsAt ? 'Choose an end time.' : startsAt && new Date(endsAt) <= new Date(startsAt) ? 'The session must end after it starts.' : '',
	};
});

function submit() {
	submitted.value = true;
	if (Object.values(errors.value).some(Boolean)) return;
	emit('submit', {
		...draft.value,
		title: draft.value.title.trim(),
		startsAt: new Date(draft.value.startsAt).toISOString(),
		endsAt: new Date(draft.value.endsAt).toISOString(),
	});
}
</script>

<template>
	<AppModal
		:model-value="modelValue"
		:title="session ? 'Change session' : 'Schedule a session'"
		as-form
		:busy="saving"
		@update:model-value="emit('update:modelValue', $event)"
		@submit="submit"
	>
		<FormField v-slot="{ id, invalidClass }" label="Title" required :error="errors.title">
			<input :id="id" v-model="draft.title" type="text" class="form-control" :class="invalidClass" maxlength="80" />
		</FormField>
		<div class="row">
			<div class="col-sm-6">
				<FormField v-slot="{ id, invalidClass }" label="Starts" required :error="errors.startsAt">
					<input :id="id" v-model="draft.startsAt" type="datetime-local" class="form-control" :class="invalidClass" />
				</FormField>
			</div>
			<div class="col-sm-6">
				<FormField v-slot="{ id, invalidClass }" label="Ends" required :error="errors.endsAt">
					<input :id="id" v-model="draft.endsAt" type="datetime-local" class="form-control" :class="invalidClass" />
				</FormField>
			</div>
		</div>
		<FormField v-slot="{ id }" label="Meeting mode" required>
			<select :id="id" v-model="draft.meetingMode" class="form-select">
				<option v-for="mode in SESSION_MEETING_MODES" :key="mode.value" :value="mode.value">{{ mode.label }}</option>
			</select>
		</FormField>
		<FormField v-slot="{ id }" label="Location" hint="A room, or a meeting link for online sessions.">
			<input :id="id" v-model="draft.location" type="text" class="form-control" maxlength="200" />
		</FormField>
		<FormField v-slot="{ id }" label="Note">
			<textarea :id="id" v-model="draft.note" class="form-control" rows="2" maxlength="500"></textarea>
		</FormField>
		<template #footer>
			<button type="button" class="btn btn-light border" :disabled="saving" @click="emit('update:modelValue', false)">Close</button>
			<button type="submit" class="btn btn-primary" :disabled="saving">{{ session ? 'Save changes' : 'Schedule session' }}</button>
		</template>
	</AppModal>
</template>
