<script setup>
defineProps({ message: { type: String, required: true } });
defineEmits(['retry']);
</script>

<template>
	<div class="alert alert-danger d-flex flex-wrap align-items-center justify-content-between gap-2" role="alert">
		<span><i class="bi bi-exclamation-circle me-2"></i>{{ message }}</span>
		<button type="button" class="btn btn-sm btn-outline-danger" @click="$emit('retry')">Try again</button>
	</div>
</template>
