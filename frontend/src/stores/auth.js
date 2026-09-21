import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { authApi } from '../api/auth.api';
import { ROLES } from '../utils/constants';

const STORAGE_KEY = 'studybuddy-session';

function readSession() {
	try {
		return JSON.parse(localStorage.getItem(STORAGE_KEY)) ?? {};
	} catch {
		return {};
	}
}

function writeSession(session) {
	try {
		if (session) localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
		else localStorage.removeItem(STORAGE_KEY);
	} catch {
		// Storage may be unavailable; the session then lasts until the tab closes.
	}
}

/** Signed-in user and token. */
export const useAuthStore = defineStore('auth', () => {
	const saved = readSession();
	const token = ref(saved.token ?? null);
	const user = ref(saved.user ?? null);

	const isAuthenticated = computed(() => Boolean(token.value && user.value));
	const isAdmin = computed(() => user.value?.role === ROLES.ADMIN);
	const homePath = computed(() => (isAdmin.value ? '/admin/users' : '/matches'));

	async function login(email, password) {
		const session = await authApi.login(email, password);
		token.value = session.token;
		user.value = session.user;
		writeSession({ token: token.value, user: user.value });
	}

	function logout() {
		token.value = null;
		user.value = null;
		writeSession(null);
	}

	/** Keep the navbar name in sync after the student edits their profile. */
	function setDisplayName(name) {
		if (!user.value) return;
		user.value = { ...user.value, name };
		writeSession({ token: token.value, user: user.value });
	}

	return { token, user, isAuthenticated, isAdmin, homePath, login, logout, setDisplayName };
});
