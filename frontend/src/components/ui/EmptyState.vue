<script setup>
defineProps({
	icon: { type: String, default: 'bi-inbox' },
	title: { type: String, required: true },
	message: { type: String, default: '' },
});
</script>

<template>
	<div class="text-center py-5 px-3">
		<div class="empty-icon mb-3"><i class="bi" :class="icon"></i></div>
		<h2 class="h6 mb-1">{{ title }}</h2>
		<p v-if="message" class="text-muted mb-3 mx-auto empty-message">{{ message }}</p>
		<slot />
	</div>
</template>

<style scoped>
.empty-icon {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	width: 3.25rem;
	height: 3.25rem;
	border-radius: 50%;
	background: #eef0f6;
	color: #7b8294;
	font-size: 1.4rem;
}
.empty-message {
	max-width: 26rem;
}
</style>
