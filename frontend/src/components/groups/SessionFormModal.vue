<script setup>
import { computed, ref, watch } from 'vue';
import { SESSION_MEETING_MODES } from '../../utils/constants';
import { endAfterStartChange, toLocalInput } from '../../utils/calendar';
import AppModal from '../ui/AppModal.vue';
import FormField from '../ui/FormField.vue';

/** Form for scheduling or changing a study session. `session` edits it; `defaults` pre-fills a new one. */
const props = defineProps({
	modelValue: Boolean,
	session: { type: Object, default: null },
	defaults: { type: Object, default: null },
	saving: Boolean,
	/** The group has a working Telegram chat, so its video chat can be offered. */
	canUseVideoChat: Boolean,
});
const emit = defineEmits(['update:modelValue', 'submit']);

const draft = ref({});
const submitted = ref(false);
/** An online session only shows Location if it already has one, so clearing the field doesn't make it vanish while typing. */
const keepLocation = ref(false);

watch(() => props.modelValue, (open) => {
	if (!open) return;
	submitted.value = false;
	keepLocation.value = Boolean(props.session?.location);
	draft.value = props.session
		? {
			title: props.session.title,
			startsAt: toLocalInput(props.session.startsAt),
			endsAt: toLocalInput(props.session.endsAt),
			meetingMode: props.session.meetingMode,
			location: props.session.location ?? '',
			note: props.session.note ?? '',
			meetingLink: props.session.meetingLink ?? '',
			groupVideoChat: props.session.groupVideoChat,
		}
		: { title: '', startsAt: '', endsAt: '', meetingMode: 'IN_PERSON', location: '', note: '', meetingLink: '', groupVideoChat: false, ...props.defaults };
}, { immediate: true });

const isOnline = computed(() => draft.value.meetingMode === 'ONLINE');
const showLocation = computed(() => !isOnline.value || keepLocation.value);
watch(isOnline, (online) => {
	if (online) keepLocation.value = Boolean(draft.value.location?.trim());
});
/** Moving the start clears everyone's answers, because they were for the old time. */
const startMoved = computed(() => {
	if (!props.session || !draft.value.startsAt) return false;
	return new Date(draft.value.startsAt).getTime() !== new Date(toLocalInput(props.session.startsAt)).getTime();
});
const answersToClear = computed(() => (props.session?.goingCount ?? 0) + (props.session?.notGoingCount ?? 0));
/** Stays visible for a session already using it, even if the chat has since gone, so the leader can change it. */
const showVideoChat = computed(() => isOnline.value && (props.canUseVideoChat || draft.value.groupVideoChat));

const errors = computed(() => {
	if (!submitted.value) return {};
	const { title, startsAt, endsAt, meetingLink, groupVideoChat } = draft.value;
	return {
		title: title?.trim() ? '' : 'Give the session a title.',
		startsAt: startsAt ? '' : 'Choose a start time.',
		endsAt: !endsAt ? 'Choose an end time.' : startsAt && new Date(endsAt) <= new Date(startsAt) ? 'The session must end after it starts.' : '',
		meetingLink: isOnline.value && !groupVideoChat && meetingLink?.trim() && !/^https?:\/\/\S+$/i.test(meetingLink.trim()) ? 'Enter a full meeting link starting with https://.' : '',
	};
});

function setStart(value) {
	draft.value.endsAt = endAfterStartChange(value, draft.value.startsAt, draft.value.endsAt);
	draft.value.startsAt = value;
}

function submit() {
	submitted.value = true;
	if (Object.values(errors.value).some(Boolean)) return;
	emit('submit', {
		...draft.value,
		title: draft.value.title.trim(),
		startsAt: new Date(draft.value.startsAt).toISOString(),
		endsAt: new Date(draft.value.endsAt).toISOString(),
		meetingLink: isOnline.value && !draft.value.groupVideoChat ? draft.value.meetingLink.trim() : '',
		groupVideoChat: isOnline.value && draft.value.groupVideoChat,
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
					<input :id="id" :value="draft.startsAt" type="datetime-local" class="form-control" :class="invalidClass" @input="setStart($event.target.value)" />
				</FormField>
			</div>
			<div class="col-sm-6">
				<FormField v-slot="{ id, invalidClass }" label="Ends" required :error="errors.endsAt">
					<input :id="id" v-model="draft.endsAt" type="datetime-local" class="form-control" :class="invalidClass" />
				</FormField>
			</div>
		</div>
		<p v-if="startMoved && answersToClear" class="alert alert-warning small" role="status">Moving the start clears the {{ answersToClear }} {{ answersToClear === 1 ? 'answer' : 'answers' }} so far, and everyone is asked again.</p>
		<FormField v-slot="{ id }" label="Meeting mode" required>
			<select :id="id" v-model="draft.meetingMode" class="form-select">
				<option v-for="mode in SESSION_MEETING_MODES" :key="mode.value" :value="mode.value">{{ mode.label }}</option>
			</select>
		</FormField>
		<template v-if="isOnline">
			<fieldset v-if="showVideoChat" class="mb-3">
				<legend class="form-label fs-6 mb-1">How will people join?</legend>
				<div class="form-check">
					<input id="session-join-link" v-model="draft.groupVideoChat" class="form-check-input" type="radio" name="session-join" :value="false" />
					<label class="form-check-label" for="session-join-link">Meeting link</label>
				</div>
				<div class="form-check">
					<input id="session-join-telegram" v-model="draft.groupVideoChat" class="form-check-input" type="radio" name="session-join" :value="true" />
					<label class="form-check-label" for="session-join-telegram">Telegram video chat</label>
				</div>
				<div v-if="draft.groupVideoChat" class="form-text">A chat admin starts the call in Telegram, then the bot tags everyone going. Bots can't start a call themselves.</div>
			</fieldset>
			<FormField v-if="!draft.groupVideoChat" v-slot="{ id, invalidClass }" label="Meeting link" hint="Create the call in Zoom, Meet or Teams, then paste its link." :error="errors.meetingLink">
				<input :id="id" v-model="draft.meetingLink" type="url" class="form-control" :class="invalidClass" maxlength="500" placeholder="https://" />
			</FormField>
		</template>
		<FormField v-if="showLocation" v-slot="{ id }" label="Location" :hint="isOnline ? 'Optional.' : 'A room or place to meet.'">
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
