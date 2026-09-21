<script setup>
const props = defineProps({
	modelValue: { type: Array, default: () => [] },
	/** [{ value, label, title? }] */
	options: { type: Array, required: true },
	disabled: Boolean,
});
const emit = defineEmits(['update:modelValue']);

function toggle(value) {
	const next = props.modelValue.includes(value)
		? props.modelValue.filter((item) => item !== value)
		: [...props.modelValue, value];
	emit('update:modelValue', next);
}
</script>

<template>
	<div class="d-flex flex-wrap gap-2" role="group">
		<button
			v-for="option in options"
			:key="option.value"
			type="button"
			class="chip"
			:class="{ active: modelValue.includes(option.value) }"
			:aria-pressed="modelValue.includes(option.value)"
			:title="option.title"
			:disabled="disabled"
			@click="toggle(option.value)"
		>
			<i v-if="modelValue.includes(option.value)" class="bi bi-check2"></i>{{ option.label }}
		</button>
	</div>
</template>
