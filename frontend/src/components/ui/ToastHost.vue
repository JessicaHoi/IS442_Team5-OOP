<script setup>
import { useToast } from '../../composables/useToast';

const { toasts, dismiss } = useToast();
</script>

<template>
	<div class="toast-container position-fixed bottom-0 end-0 p-3" aria-live="polite" aria-atomic="true">
		<div
			v-for="toast in toasts"
			:key="toast.id"
			class="toast show align-items-center border-0 shadow-sm mb-2"
			role="status"
		>
			<div class="d-flex">
				<div class="toast-body d-flex align-items-start gap-2">
					<i
						class="bi mt-1"
						:class="toast.type === 'danger' ? 'bi-exclamation-circle-fill text-danger' : 'bi-check-circle-fill text-success'"
					></i>
					<span>{{ toast.message }}</span>
				</div>
				<button type="button" class="btn-close me-2 m-auto" aria-label="Dismiss" @click="dismiss(toast.id)"></button>
			</div>
		</div>
	</div>
</template>
