import { createApp } from 'vue';
import { createPinia } from 'pinia';

import 'bootstrap-icons/font/bootstrap-icons.css';
import './assets/styles/theme.scss';
// Registers Bootstrap's data-attribute behaviour for the navbar and dropdown menu.
import 'bootstrap/js/dist/collapse';
import 'bootstrap/js/dist/dropdown';

import App from './App.vue';
import router from './router';
import { configureHttp } from './api/http';
import { useAuthStore } from './stores/auth';

const app = createApp(App);
app.use(createPinia());

const auth = useAuthStore();
configureHttp({
	getToken: () => auth.token,
	onUnauthorized: () => {
		auth.logout();
		router.push({ name: 'login', query: { expired: '1' } });
	},
});

app.use(router);
app.mount('#app');
