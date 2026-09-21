<script setup>
defineProps({ label: { type: String, default: 'Loading…' } });
</script>

<template>
	<div class="d-flex align-items-center justify-content-center gap-2 text-muted py-5" role="status">
		<span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
		<span>{{ label }}</span>
	</div>
</template>
