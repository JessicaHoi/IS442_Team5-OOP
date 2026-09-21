import { ref } from 'vue';

/**
 * Track loading and error state for a data-loading function.
 * `run` never throws: on failure it stores the message in `error`.
 */
export function useAsync(loader) {
	const loading = ref(false);
	const error = ref('');

	async function run(...args) {
		loading.value = true;
		error.value = '';
		try {
			return await loader(...args);
		} catch (caught) {
			error.value = caught.message;
			return undefined;
		} finally {
			loading.value = false;
		}
	}

	return { loading, error, run };
}
