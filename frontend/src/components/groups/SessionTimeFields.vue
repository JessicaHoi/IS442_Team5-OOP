<script setup>
import { computed, nextTick, ref } from 'vue';
import { VueDatePicker } from '@vuepic/vue-datepicker';
import '@vuepic/vue-datepicker/dist/main.css';
import { endAfterStartChange, endTimeOptions, startTimeOptions, toLocalInput } from '../../utils/calendar';
import FormField from '../ui/FormField.vue';

/** A session's date, start and end. One date is enough: the end's list runs past midnight. Values are datetime-local strings. */
const props = defineProps({
	startsAt: { type: String, required: true },
	endsAt: { type: String, required: true },
	startError: { type: String, default: '' },
	endError: { type: String, default: '' },
});
const emit = defineEmits(['update:startsAt', 'update:endsAt']);

const today = new Date();
const calendarOpen = ref(false);
const dateField = ref(null);
const calendarFormats = {
	input: (date) => date.toLocaleDateString('en-SG', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' }),
};

const startOptions = computed(() => startTimeOptions(props.startsAt));
const endOptions = computed(() => endTimeOptions(props.startsAt, props.endsAt));
const date = computed(() => new Date(props.startsAt));

/** Moving the start keeps the session's length. */
function setStart(value) {
	emit('update:endsAt', endAfterStartChange(value, props.startsAt, props.endsAt));
	emit('update:startsAt', value);
}

function setDate(value) {
	if (value) setStart(`${toLocalInput(value).slice(0, 10)}${props.startsAt.slice(10)}`);
}

/** Escape should close the calendar without also closing the modal around it. */
function keepEscapeInCalendar(event) {
	if (calendarOpen.value) event.stopPropagation();
}

/** The calendar drops focus when it closes, which loses a keyboard user's place. Focusing its input would reopen it, so the wrapper takes focus. A click on another field has already moved it. */
async function calendarClosed() {
	calendarOpen.value = false;
	await nextTick();
	const focused = document.activeElement;
	if (focused === document.body || focused.closest('.dp--menu')) dateField.value?.focus();
}
</script>

<template>
	<FormField v-slot="{ id }" label="Date" required>
		<div ref="dateField" class="session-date" tabindex="-1" @keydown.esc="keepEscapeInCalendar">
			<VueDatePicker
				:model-value="date"
				:min-date="today"
				:formats="calendarFormats"
				:time-config="{ enableTimePicker: false }"
				:input-attrs="{ id, clearable: false }"
				:aria-labels="{ input: 'Date' }"
				:action-row="{ showSelect: false, showCancel: false, showNow: false, showPreview: false }"
				:week-start="1"
				auto-apply
				arrow-navigation
				hide-offset-dates
				@update:model-value="setDate"
				@open="calendarOpen = true"
				@closed="calendarClosed"
			/>
		</div>
	</FormField>
	<div class="row">
		<div class="col-5">
			<FormField v-slot="{ id, invalidClass }" label="Starts" required :error="startError">
				<select :id="id" :value="startsAt" class="form-select" :class="invalidClass" @change="setStart($event.target.value)">
					<option v-for="option in startOptions" :key="option.value" :value="option.value" :disabled="option.disabled">{{ option.label }}</option>
				</select>
			</FormField>
		</div>
		<div class="col-7">
			<FormField v-slot="{ id, invalidClass }" label="Ends" required :error="endError">
				<select :id="id" :value="endsAt" class="form-select" :class="invalidClass" @change="emit('update:endsAt', $event.target.value)">
					<option v-for="option in endOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
				</select>
			</FormField>
		</div>
	</div>
</template>

<style scoped>
.session-date:focus {
	outline: none;
}

/* The calendar's own theme, pointed at the app's colours. */
.session-date :deep(.dp--theme-light) {
	--dp-font-family: var(--bs-body-font-family);
	--dp-primary-color: var(--bs-primary);
	--dp-border-color: var(--bs-border-color);
	--dp-border-color-hover: var(--bs-border-color);
	--dp-border-color-focus: var(--bs-primary);
	--dp-menu-border-color: var(--bs-border-color);
	--dp-border-radius: 0.5rem;
	--dp-cell-border-radius: 0.375rem;
}
</style>
