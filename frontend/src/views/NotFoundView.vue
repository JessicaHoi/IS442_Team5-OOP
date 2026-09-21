<script setup>
import { RouterLink } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import EmptyState from '../components/ui/EmptyState.vue';

const auth = useAuthStore();
</script>

<template>
	<EmptyState icon="bi-compass" title="Page not found" message="The page you are looking for does not exist or has moved.">
		<RouterLink class="btn btn-primary" :to="auth.homePath">Go to home</RouterLink>
	</EmptyState>
</template>
