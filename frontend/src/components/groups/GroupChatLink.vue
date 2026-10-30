<script setup>
import { computed, ref, watch } from 'vue';
import { INVALID_CHAT_LINK_MESSAGE, isValidChatLink } from '../../utils/telegram';

/** Lets the leader paste in the group's Telegram chat link, or remove it. */
const props = defineProps({
	link: { type: String, default: null },
	saving: { type: Boolean, default: false },
});
const emit = defineEmits(['save', 'clear']);

const url = ref(props.link ?? '');
const submitted = ref(false);
watch(() => props.link, (value) => { url.value = value ?? ''; submitted.value = false; });

const error = computed(() => (submitted.value && !isValidChatLink(url.value) ? INVALID_CHAT_LINK_MESSAGE : ''));

function save() {
	submitted.value = true;
	if (isValidChatLink(url.value)) emit('save', url.value.trim());
}
</script>

<template>
	<section aria-labelledby="chat-link-title">
		<h2 id="chat-link-title" class="h5 mb-1"><i class="bi bi-telegram me-2"></i>Group chat</h2>
		<p class="text-body-secondary small mb-3">Paste your Telegram group's invite link. Only group members can see it.</p>
		<form class="d-flex flex-wrap gap-2" novalidate @submit.prevent="save">
			<div class="flex-grow-1">
				<label class="visually-hidden" for="chat-link-input">Telegram invite link</label>
				<input id="chat-link-input" v-model="url" type="url" class="form-control" :class="{ 'is-invalid': error }" placeholder="https://t.me/+…" />
				<div v-if="error" class="invalid-feedback">{{ error }}</div>
			</div>
			<button type="submit" class="btn btn-primary align-self-start" :disabled="saving">Save link</button>
			<button v-if="link" type="button" class="btn btn-light border align-self-start" :disabled="saving" @click="emit('clear')">Remove</button>
		</form>
	</section>
</template>
