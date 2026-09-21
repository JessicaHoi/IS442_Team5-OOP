<script setup>
import { computed, watch } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import StudentAvatar from '../components/ui/StudentAvatar.vue';

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

const appName = import.meta.env.VITE_APP_NAME || 'StudyBuddy';

const STUDENT_LINKS = [
	{ to: '/matches', label: 'Find buddies', icon: 'bi-search-heart' },
	{ to: '/connections', label: 'Connections', icon: 'bi-people' },
	{ to: '/groups', label: 'Study groups', icon: 'bi-collection' },
	{ to: '/profile', label: 'Profile', icon: 'bi-person' },
];
const ADMIN_LINKS = [
	{ to: '/admin/users', label: 'Users', icon: 'bi-person-gear' },
	{ to: '/admin/matching', label: 'Matching', icon: 'bi-sliders' },
];

const links = computed(() => (auth.isAdmin ? ADMIN_LINKS : STUDENT_LINKS));
/** Highlight a nav item for its own page and every page beneath it. */
const isSection = (path) => route.path === path || route.path.startsWith(`${path}/`);
const roleLabel = computed(() => (auth.isAdmin ? 'System administrator' : 'Student'));

// Collapse the mobile menu after navigating.
watch(
	() => route.fullPath,
	() => document.getElementById('main-nav')?.classList.remove('show'),
);

function signOut() {
	auth.logout();
	router.push({ name: 'login' });
}
</script>

<template>
	<div class="d-flex flex-column min-vh-100">
		<nav class="navbar navbar-expand-md bg-white border-bottom sticky-top py-2">
			<div class="container">
				<RouterLink class="navbar-brand d-flex align-items-center gap-2 fw-semibold me-4" :to="auth.homePath">
					<span class="brand-mark"><i class="bi bi-people-fill"></i></span>
					{{ appName }}
				</RouterLink>

				<button
					class="navbar-toggler border-0 px-1"
					type="button"
					data-bs-toggle="collapse"
					data-bs-target="#main-nav"
					aria-controls="main-nav"
					aria-expanded="false"
					aria-label="Toggle navigation"
				>
					<span class="navbar-toggler-icon"></span>
				</button>

				<div id="main-nav" class="collapse navbar-collapse">
					<ul class="navbar-nav me-auto gap-md-1 mt-2 mt-md-0">
						<li v-for="link in links" :key="link.to" class="nav-item">
							<RouterLink :to="link.to" class="nav-link d-flex align-items-center gap-2 px-md-3" :class="{ active: isSection(link.to) }">
								<i class="bi" :class="link.icon"></i>{{ link.label }}
							</RouterLink>
						</li>
					</ul>

					<div v-if="auth.user" class="dropdown my-2 my-md-0">
						<button
							class="btn btn-light border d-flex align-items-center gap-2 py-1 ps-1 pe-3"
							type="button"
							data-bs-toggle="dropdown"
							aria-expanded="false"
						>
							<StudentAvatar :name="auth.user.name" :size="28" />
							<span class="small fw-medium">{{ auth.user.name }}</span>
						</button>
						<ul class="dropdown-menu dropdown-menu-end shadow-sm">
							<li class="px-3 py-2">
								<div class="small fw-medium">{{ auth.user.email }}</div>
								<div class="small text-muted">{{ roleLabel }}</div>
							</li>
							<li><hr class="dropdown-divider" /></li>
							<li>
								<button class="dropdown-item d-flex align-items-center gap-2" type="button" @click="signOut">
									<i class="bi bi-box-arrow-right"></i>Sign out
								</button>
							</li>
						</ul>
					</div>
				</div>
			</div>
		</nav>

		<main class="container py-4 py-md-5 flex-grow-1">
			<RouterView />
		</main>
	</div>
</template>

<style scoped>
.brand-mark {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	width: 1.9rem;
	height: 1.9rem;
	border-radius: 0.5rem;
	background: var(--bs-primary);
	color: #fff;
	font-size: 1rem;
}
.nav-link {
	border-radius: 0.5rem;
	color: var(--bs-secondary-color);
	font-weight: 500;
}
.nav-link:hover {
	color: var(--bs-body-color);
}
.nav-link.active {
	color: var(--bs-primary);
	background: rgba(79, 70, 229, 0.08);
}
</style>
