<script setup>
import { computed } from 'vue';
import { DAYS } from '../../utils/constants';
import { validateSlots } from '../../utils/availability';

const props = defineProps({
	modelValue: { type: Array, default: () => [] },
	disabled: Boolean,
});
const emit = defineEmits(['update:modelValue']);

const errors = computed(() => validateSlots(props.modelValue));

function update(index, field, value) {
	emit(
		'update:modelValue',
		props.modelValue.map((slot, i) => (i === index ? { ...slot, [field]: value } : slot)),
	);
}

function addSlot() {
	const usedDays = new Set(props.modelValue.map((slot) => slot.day));
	const day = DAYS.find((d) => !usedDays.has(d.value))?.value ?? 'MON';
	emit('update:modelValue', [...props.modelValue, { day, start: '19:00', end: '21:00' }]);
}

function removeSlot(index) {
	emit(
		'update:modelValue',
		props.modelValue.filter((_, i) => i !== index),
	);
}
</script>

<template>
	<div>
		<p v-if="modelValue.length === 0" class="text-muted small mb-2">No time slots yet. Add at least one.</p>

		<div v-for="(slot, index) in modelValue" :key="index" class="mb-2">
			<div class="row g-2 align-items-center">
				<div class="col-12 col-sm-4">
					<select
						class="form-select"
						:class="{ 'is-invalid': errors[index] }"
						:value="slot.day"
						:disabled="disabled"
						:aria-label="`Day for slot ${index + 1}`"
						@change="update(index, 'day', $event.target.value)"
					>
						<option v-for="day in DAYS" :key="day.value" :value="day.value">{{ day.long }}</option>
					</select>
				</div>
				<div class="col-5 col-sm-3">
					<input
						type="time"
						step="900"
						class="form-control"
						:class="{ 'is-invalid': errors[index] }"
						:value="slot.start"
						:disabled="disabled"
						:aria-label="`Start time for slot ${index + 1}`"
						@input="update(index, 'start', $event.target.value)"
					/>
				</div>
				<div class="col-5 col-sm-3">
					<input
						type="time"
						step="900"
						class="form-control"
						:class="{ 'is-invalid': errors[index] }"
						:value="slot.end"
						:disabled="disabled"
						:aria-label="`End time for slot ${index + 1}`"
						@input="update(index, 'end', $event.target.value)"
					/>
				</div>
				<div class="col-2 col-sm-2">
					<button
						type="button"
						class="btn btn-icon btn-outline-secondary"
						:disabled="disabled"
						:aria-label="`Remove slot ${index + 1}`"
						@click="removeSlot(index)"
					>
						<i class="bi bi-x-lg"></i>
					</button>
				</div>
			</div>
			<div v-if="errors[index]" class="invalid-feedback d-block">{{ errors[index] }}</div>
		</div>

		<button type="button" class="btn btn-sm btn-outline-primary mt-1" :disabled="disabled" @click="addSlot">
			<i class="bi bi-plus-lg me-1"></i>Add time slot
		</button>
	</div>
</template>
