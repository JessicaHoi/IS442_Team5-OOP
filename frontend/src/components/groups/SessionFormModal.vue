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
	/** The group has a working Telegram chat, so its video chat can be offered. */
	canUseVideoChat: Boolean,
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
			meetingLink: props.session.meetingLink ?? '',
			groupVideoChat: props.session.groupVideoChat,
		}
		: { title: '', startsAt: '', endsAt: '', meetingMode: 'IN_PERSON', location: '', note: '', meetingLink: '', groupVideoChat: false, ...props.defaults };
}, { immediate: true });

const isOnline = computed(() => draft.value.meetingMode === 'ONLINE');
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
		<template v-if="isOnline">
			<div v-if="showVideoChat" class="form-check mb-3">
				<input id="session-video-chat" v-model="draft.groupVideoChat" class="form-check-input" type="checkbox" />
				<label class="form-check-label" for="session-video-chat">Use the group's Telegram video chat</label>
				<div class="form-text">A chat admin starts the call in Telegram. The bot then tags everyone going who is in the chat, because Telegram doesn't let bots start a call or invite people to one.</div>
			</div>
			<FormField v-if="!draft.groupVideoChat" v-slot="{ id, invalidClass }" label="Meeting link" hint="Create the call in Zoom, Meet or Teams, then paste its link." :error="errors.meetingLink">
				<input :id="id" v-model="draft.meetingLink" type="url" class="form-control" :class="invalidClass" maxlength="500" placeholder="https://" />
			</FormField>
		</template>
		<FormField v-slot="{ id }" label="Location" :hint="isOnline ? 'Optional.' : 'A room or place to meet.'">
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
