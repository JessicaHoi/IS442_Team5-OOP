import { fileURLToPath, URL } from 'node:url';
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

/**
 * Build settings are read from environment variables (see .env.example)
 * so nothing environment-specific is hard-coded here.
 */
export default defineConfig(({ mode }) => {
	const env = loadEnv(mode, process.cwd(), '');
	const backendUrl = env.BACKEND_URL || 'http://localhost:8080';
	// In production the SPA is packaged inside the Spring Boot jar (monolith).
	const buildOutDir = env.BUILD_OUT_DIR || '../backend/src/main/resources/static';

	return {
		plugins: [vue()],
		resolve: {
			alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
		},
		css: {
			preprocessorOptions: {
				scss: {
					// Bootstrap 5 still uses Sass @import internally.
					quietDeps: true,
					silenceDeprecations: ['import', 'global-builtin', 'color-functions', 'if-function'],
				},
			},
		},
		server: {
			port: 5173,
			proxy: { '/api': { target: backendUrl, changeOrigin: true } },
		},
		build: {
			outDir: buildOutDir,
			emptyOutDir: true,
		},
		test: {
			environment: 'node',
			include: ['src/**/*.test.js'],
		},
	};
});
