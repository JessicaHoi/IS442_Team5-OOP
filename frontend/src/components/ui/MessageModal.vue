<script setup>
/** Small dialog that collects an optional message, used for buddy and group-join requests. */
import { ref, watch } from 'vue';
import AppModal from './AppModal.vue';
import { MESSAGE_MAX_LENGTH } from '../../utils/constants';

const props = defineProps({
	modelValue: Boolean,
	title: { type: String, required: true },
	subtitle: { type: String, default: '' },
	placeholder: { type: String, default: 'Add a short message (optional)' },
	submitLabel: { type: String, default: 'Send request' },
	busy: Boolean,
});
const emit = defineEmits(['update:modelValue', 'submit']);

const message = ref('');

watch(
	() => props.modelValue,
	(open) => open && (message.value = ''),
);
</script>

<template>
	<AppModal :model-value="modelValue" :title="title" as-form @update:model-value="emit('update:modelValue', $event)" @submit="emit('submit', message.trim())">
		<p v-if="subtitle" class="text-muted">{{ subtitle }}</p>
		<label for="request-message" class="form-label">Message <span class="text-muted fw-normal">(optional)</span></label>
		<textarea
			id="request-message"
			v-model="message"
			class="form-control"
			rows="4"
			:maxlength="MESSAGE_MAX_LENGTH"
			:placeholder="placeholder"
		></textarea>
		<div class="form-text text-end">{{ message.length }}/{{ MESSAGE_MAX_LENGTH }}</div>
		<template #footer>
			<button type="button" class="btn btn-light" data-bs-dismiss="modal">Cancel</button>
			<button type="submit" class="btn btn-primary" :disabled="busy">
				<span v-if="busy" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>{{ submitLabel }}
			</button>
		</template>
	</AppModal>
</template>
