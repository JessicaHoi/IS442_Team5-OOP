<script setup>
import { RouterView } from 'vue-router';

const appName = import.meta.env.VITE_APP_NAME || 'StudyBuddy';
</script>

<template>
	<div class="min-vh-100 d-flex flex-column align-items-center justify-content-center px-3 py-5">
		<div class="text-center mb-4">
			<span class="brand-mark mb-3"><i class="bi bi-people-fill"></i></span>
			<h1 class="h4 mb-1">{{ appName }}</h1>
			<p class="text-muted mb-0">Find the right people to study with.</p>
		</div>
		<div class="auth-card w-100">
			<RouterView />
		</div>
	</div>
</template>

<style scoped>
.auth-card {
	max-width: 26rem;
}
.brand-mark {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	width: 3rem;
	height: 3rem;
	border-radius: 0.85rem;
	background: var(--bs-primary);
	color: #fff;
	font-size: 1.5rem;
}
</style>
