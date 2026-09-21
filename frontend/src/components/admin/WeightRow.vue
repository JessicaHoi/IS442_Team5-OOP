<script setup>
import { useId } from 'vue';

const props = defineProps({
	/** { key, enabled, weight } */
	criterion: { type: Object, required: true },
	/** { label, hint } */
	meta: { type: Object, required: true },
	/** This criterion's share of the total weight, as a percentage. */
	share: { type: Number, required: true },
});
const emit = defineEmits(['update:criterion']);

const switchId = useId();

function patch(changes) {
	emit('update:criterion', { ...props.criterion, ...changes });
}
</script>

<template>
	<div class="row align-items-center g-3 py-3 border-bottom criterion-row">
		<div class="col-md-5">
			<div class="form-check form-switch mb-0">
				<input
					:id="switchId"
					class="form-check-input"
					type="checkbox"
					role="switch"
					:checked="criterion.enabled"
					@change="patch({ enabled: $event.target.checked })"
				/>
				<label class="form-check-label fw-medium" :for="switchId">{{ meta.label }}</label>
			</div>
			<div class="small text-muted ms-md-5 ps-md-1">{{ meta.hint }}</div>
		</div>
		<div class="col-8 col-md-5">
			<input
				type="range"
				class="form-range"
				min="0"
				max="100"
				step="5"
				:value="criterion.weight"
				:disabled="!criterion.enabled"
				:aria-label="`${meta.label} weight`"
				@input="patch({ weight: Number($event.target.value) })"
			/>
		</div>
		<div class="col-4 col-md-2 text-end">
			<span class="fw-semibold">{{ criterion.enabled ? criterion.weight : '–' }}</span>
			<span class="badge badge-soft-primary ms-2" :class="{ invisible: !criterion.enabled }">{{ share }}%</span>
		</div>
	</div>
</template>

<style scoped>
.criterion-row:last-child {
	border-bottom: 0 !important;
}
</style>
