<script setup>
defineProps({
	title: { type: String, required: true },
	subtitle: { type: String, default: '' },
});
</script>

<template>
	<header class="d-flex flex-wrap align-items-end justify-content-between gap-3 mb-4">
		<div>
			<h1 class="h3 mb-1">{{ title }}</h1>
			<p v-if="subtitle" class="text-muted mb-0">{{ subtitle }}</p>
		</div>
		<div v-if="$slots.actions" class="d-flex flex-wrap gap-2">
			<slot name="actions" />
		</div>
	</header>
</template>
