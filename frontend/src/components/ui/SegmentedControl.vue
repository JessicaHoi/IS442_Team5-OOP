<script setup>
import { useId } from 'vue';

defineProps({
	modelValue: { type: String, default: '' },
	/** [{ value, label }] */
	options: { type: Array, required: true },
	disabled: Boolean,
});
defineEmits(['update:modelValue']);

const name = useId();
</script>

<template>
	<div class="btn-group flex-wrap" role="radiogroup">
		<template v-for="option in options" :key="option.value">
			<input
				:id="`${name}-${option.value}`"
				type="radio"
				class="btn-check"
				:name="name"
				:value="option.value"
				:checked="modelValue === option.value"
				:disabled="disabled"
				@change="$emit('update:modelValue', option.value)"
			/>
			<label class="btn btn-outline-primary" :for="`${name}-${option.value}`">{{ option.label }}</label>
		</template>
	</div>
</template>
