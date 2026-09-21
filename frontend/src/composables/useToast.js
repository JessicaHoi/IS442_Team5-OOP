import { ref } from 'vue';

const TOAST_DURATION_MS = 4000;

const toasts = ref([]);
let nextId = 1;

function dismiss(id) {
	toasts.value = toasts.value.filter((toast) => toast.id !== id);
}

function push(type, message) {
	const id = nextId++;
	toasts.value = [...toasts.value, { id, type, message }];
	setTimeout(() => dismiss(id), TOAST_DURATION_MS);
}

/** App-wide toast notifications. */
export function useToast() {
	return {
		toasts,
		dismiss,
		success: (message) => push('success', message),
		error: (message) => push('danger', message),
	};
}
