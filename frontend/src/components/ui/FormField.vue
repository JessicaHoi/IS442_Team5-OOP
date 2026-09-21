<script setup>
import { useId } from 'vue';

defineProps({
	label: { type: String, required: true },
	error: { type: String, default: '' },
	hint: { type: String, default: '' },
	required: Boolean,
});

const id = useId();
</script>

<template>
	<div class="mb-3">
		<label :for="id" class="form-label">
			{{ label }}<span v-if="required" class="text-danger ms-1" aria-hidden="true">*</span>
		</label>
		<slot :id="id" :invalid-class="error ? 'is-invalid' : ''" />
		<div v-if="error" class="invalid-feedback d-block">{{ error }}</div>
		<div v-else-if="hint" class="form-text">{{ hint }}</div>
	</div>
</template>
