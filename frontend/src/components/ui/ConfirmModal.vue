<script setup>
import AppModal from './AppModal.vue';
import { useConfirm } from '../../composables/useConfirm';

const { state, settle } = useConfirm();

function onVisibilityChange(open) {
	if (!open) settle(false);
}
</script>

<template>
	<AppModal :model-value="state.open" :title="state.title" size="sm" @update:model-value="onVisibilityChange">
		<p v-if="state.message" class="text-muted mb-0">{{ state.message }}</p>
		<template #footer>
			<button type="button" class="btn btn-light" @click="settle(false)">Cancel</button>
			<button type="button" class="btn" :class="state.danger ? 'btn-danger' : 'btn-primary'" @click="settle(true)">
				{{ state.confirmLabel }}
			</button>
		</template>
	</AppModal>
</template>
