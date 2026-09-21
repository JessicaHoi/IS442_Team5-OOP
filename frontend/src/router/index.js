import { createRouter, createWebHashHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { ROLES } from '../utils/constants';
import AppShell from '../layouts/AppShell.vue';
import AuthLayout from '../layouts/AuthLayout.vue';

const student = { role: ROLES.STUDENT };
const admin = { role: ROLES.ADMIN };

const routes = [
	{
		path: '/login',
		component: AuthLayout,
		children: [
			{
				path: '',
				name: 'login',
				component: () => import('../views/LoginView.vue'),
				meta: { public: true, title: 'Sign in' },
			},
		],
	},
	{
		path: '/',
		component: AppShell,
		children: [
			{ path: '', redirect: () => useAuthStore().homePath },
			{
				path: 'matches',
				name: 'matches',
				component: () => import('../views/student/MatchesView.vue'),
				meta: { ...student, title: 'Find buddies' },
			},
			{
				path: 'connections',
				name: 'connections',
				component: () => import('../views/student/ConnectionsView.vue'),
				meta: { ...student, title: 'Connections' },
			},
			{
				path: 'groups',
				name: 'groups',
				component: () => import('../views/student/GroupsView.vue'),
				meta: { ...student, title: 'Study groups' },
			},
			{
				path: 'groups/new',
				name: 'group-new',
				component: () => import('../views/leader/GroupCreateView.vue'),
				meta: { ...student, title: 'Create group' },
			},
			{
				path: 'groups/:id/manage',
				name: 'group-manage',
				component: () => import('../views/leader/GroupManageView.vue'),
				props: (route) => ({ id: Number(route.params.id) }),
				meta: { ...student, title: 'Manage group' },
			},
			{
				path: 'profile',
				name: 'profile',
				component: () => import('../views/student/ProfileView.vue'),
				meta: { ...student, title: 'My profile' },
			},
			{
				path: 'admin/users',
				name: 'admin-users',
				component: () => import('../views/admin/AdminUsersView.vue'),
				meta: { ...admin, title: 'Users' },
			},
			{
				path: 'admin/matching',
				name: 'admin-matching',
				component: () => import('../views/admin/AdminMatchingView.vue'),
				meta: { ...admin, title: 'Matching configuration' },
			},
			{
				path: ':pathMatch(.*)*',
				name: 'not-found',
				component: () => import('../views/NotFoundView.vue'),
				meta: { title: 'Page not found' },
			},
		],
	},
];

const router = createRouter({
	// Hash history keeps the monolith simple: Spring Boot only has to serve index.html.
	history: createWebHashHistory(),
	routes,
	scrollBehavior: () => ({ top: 0 }),
});

router.beforeEach((to) => {
	const auth = useAuthStore();

	if (to.meta.public) return auth.isAuthenticated ? auth.homePath : true;

	if (!auth.isAuthenticated) {
		return { name: 'login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } };
	}
	if (to.meta.role && to.meta.role !== auth.user.role) return auth.homePath;
	return true;
});

router.afterEach((to) => {
	const appName = import.meta.env.VITE_APP_NAME || 'StudyBuddy';
	document.title = to.meta.title ? `${to.meta.title} · ${appName}` : appName;
});

export default router;
