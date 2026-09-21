import { reactive } from 'vue';

const state = reactive({
	open: false,
	title: '',
	message: '',
	confirmLabel: 'Confirm',
	danger: false,
});
let pendingResolve = null;

function settle(result) {
	state.open = false;
	pendingResolve?.(result);
	pendingResolve = null;
}

/**
 * Promise-based confirmation dialog.
 *   if (await confirm({ title: 'Delete?', danger: true })) { ... }
 */
export function useConfirm() {
	function confirm({ title, message = '', confirmLabel = 'Confirm', danger = false }) {
		pendingResolve?.(false);
		Object.assign(state, { open: true, title, message, confirmLabel, danger });
		return new Promise((resolve) => {
			pendingResolve = resolve;
		});
	}
	return { confirm, state, settle };
}
