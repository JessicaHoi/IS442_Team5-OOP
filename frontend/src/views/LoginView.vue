<script setup>
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { useToast } from '../composables/useToast';

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const toast = useToast();

const email = ref('');
const password = ref('');
const busy = ref(false);
const error = ref(route.query.expired ? 'Your session has ended. Please sign in again.' : '');

// Demo helpers exist only in mock mode and are left out of production builds.
const demoAccounts = ref([]);
let resetDemoData = () => {};
if (import.meta.env.VITE_USE_MOCK === 'true') {
	import('../api/mock/demoAccounts').then((module) => {
		demoAccounts.value = module.DEMO_ACCOUNTS;
		resetDemoData = module.resetDb;
	});
}

function useDemo(account) {
	email.value = account.email;
	password.value = account.password;
	error.value = '';
}

function resetDemo() {
	resetDemoData();
	toast.success('Demo data has been reset.');
}

async function submit() {
	error.value = '';
	if (!email.value.trim() || !password.value) {
		error.value = 'Enter your email and password.';
		return;
	}
	busy.value = true;
	try {
		await auth.login(email.value.trim(), password.value);
		const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') ? route.query.redirect : null;
		await router.push(redirect ?? auth.homePath);
	} catch (caught) {
		error.value = caught.message;
	} finally {
		busy.value = false;
	}
}
</script>

<template>
	<div class="card-flat p-4">
		<h2 class="h5 mb-4">Sign in</h2>

		<div v-if="error" class="alert alert-danger py-2" role="alert">{{ error }}</div>

		<form novalidate @submit.prevent="submit">
			<div class="mb-3">
				<label for="login-email" class="form-label">Email</label>
				<input id="login-email" v-model="email" type="email" class="form-control" autocomplete="username" autofocus />
			</div>
			<div class="mb-4">
				<label for="login-password" class="form-label">Password</label>
				<input id="login-password" v-model="password" type="password" class="form-control" autocomplete="current-password" />
			</div>
			<button type="submit" class="btn btn-primary w-100" :disabled="busy">
				<span v-if="busy" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Sign in
			</button>
		</form>

		<div v-if="demoAccounts.length" class="border-top mt-4 pt-3">
			<div class="section-title">Demo accounts</div>
			<div class="d-flex flex-wrap gap-2 mb-2">
				<button
					v-for="account in demoAccounts"
					:key="account.email"
					type="button"
					class="btn btn-sm btn-light border"
					@click="useDemo(account)"
				>
					{{ account.label }}
				</button>
			</div>
			<p class="small text-muted mb-1">Password for every demo account: <code>password</code></p>
			<button type="button" class="btn btn-link btn-sm p-0 text-decoration-none" @click="resetDemo">Reset demo data</button>
		</div>
	</div>
</template>
