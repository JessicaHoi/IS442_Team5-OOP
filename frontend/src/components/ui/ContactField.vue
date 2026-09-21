<script setup>
defineProps({
	/** The contact number, or null while it is still hidden. */
	value: { type: String, default: null },
});
</script>

<template>
	<span v-if="value" class="d-inline-flex align-items-center gap-2 fw-medium">
		<i class="bi bi-telephone text-success"></i>{{ value }}
	</span>
	<span v-else class="d-inline-flex align-items-center gap-2 text-muted">
		<i class="bi bi-lock"></i>Hidden until you are connected
	</span>
</template>
