<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { DAYS } from '../../utils/constants';
import { toMinutes } from '../../utils/availability';
import { BLOCK_MINUTES, GRID_TIMES, coversBlock, heatmapColor, minuteLabel, paintAvailability } from '../../utils/groupAvailability';

const props = defineProps({
	modelValue: { type: Array, default: () => [] },
	cells: { type: Array, default: () => [] },
	memberCount: { type: Number, default: 0 },
	editable: Boolean,
	disabled: Boolean,
});
const emit = defineEmits(['update:modelValue']);
const root = ref(null);
const hovered = ref(null);
const focused = ref('0-420');
let stroke = null;
const cellsByKey = computed(() => new Map(props.cells.map((cell) => [`${cell.day.slice(0, 3)}-${toMinutes(cell.startTime)}`, cell])));
const selectedKeys = computed(() => new Set(DAYS.flatMap((day) => GRID_TIMES.filter((minute) => coversBlock(props.modelValue, day.value, minute)).map((minute) => `${day.value}-${minute}`))));
const selected = (day, minute) => selectedKeys.value.has(`${day}-${minute}`);
const cellAt = (day, minute) => cellsByKey.value.get(`${day}-${minute}`) ?? { freeCount: 0, availableMembers: [] };
const blockLabel = (dayIndex, minute) => `${DAYS[dayIndex].label} ${minuteLabel(minute)}–${minuteLabel(minute + BLOCK_MINUTES)}`;
const hoverCell = computed(() => hovered.value ? cellAt(DAYS[hovered.value.dayIndex].value, hovered.value.minute) : null);

function description(dayIndex, minute) {
	const day = DAYS[dayIndex].value;
	if (props.editable) return `${blockLabel(dayIndex, minute)}: ${selected(day, minute) ? 'available' : 'unavailable'}`;
	const cell = cellAt(day, minute);
	return `${blockLabel(dayIndex, minute)}: ${cell.freeCount} of ${props.memberCount} available${cell.availableMembers.length ? ` — ${cell.availableMembers.map((member) => member.name).join(', ')}` : ''}`;
}

function endStroke(cancel = false) {
	if (!stroke) return;
	const current = stroke;
	stroke = null;
	if (root.value?.hasPointerCapture(current.pointerId)) root.value.releasePointerCapture(current.pointerId);
	if (cancel) emit('update:modelValue', current.original);
}

function startStroke(event, dayIndex, minute) {
	if (!props.editable || props.disabled || event.button !== 0) return;
	event.preventDefault();
	endStroke();
	focused.value = `${dayIndex}-${minute}`;
	event.currentTarget.focus();
	stroke = { anchor: { dayIndex, minute }, original: props.modelValue.map((slot) => ({ ...slot })), adding: !selected(DAYS[dayIndex].value, minute), pointerId: event.pointerId };
	root.value.setPointerCapture(event.pointerId);
	emit('update:modelValue', paintAvailability(stroke.original, stroke.anchor, stroke.anchor, stroke.adding));
}

function moveStroke(event) {
	if (!stroke || props.disabled || event.pointerId !== stroke.pointerId) return;
	const element = document.elementFromPoint(event.clientX, event.clientY)?.closest('[data-availability-cell]');
	if (!element || !root.value.contains(element)) return;
	const target = { dayIndex: Number(element.dataset.dayIndex), minute: Number(element.dataset.minute) };
	hovered.value = target;
	emit('update:modelValue', paintAvailability(stroke.original, stroke.anchor, target, stroke.adding));
}

function keydown(event, dayIndex, minute) {
	const deltas = { ArrowUp: [0, -30], ArrowDown: [0, 30], ArrowLeft: [-1, 0], ArrowRight: [1, 0] };
	if (!deltas[event.key]) return;
	event.preventDefault();
	const [d, m] = deltas[event.key];
	const nextDay = Math.max(0, Math.min(6, dayIndex + d));
	const nextMinute = Math.max(GRID_TIMES[0], Math.min(GRID_TIMES.at(-1), minute + m));
	focused.value = `${nextDay}-${nextMinute}`;
	root.value.querySelector(`[data-day-index="${nextDay}"][data-minute="${nextMinute}"]`)?.focus();
}

function click(event, dayIndex, minute) {
	hovered.value = { dayIndex, minute };
	if (!props.editable || props.disabled || event.detail !== 0) return;
	const point = { dayIndex, minute };
	emit('update:modelValue', paintAvailability(props.modelValue, point, point, !selected(DAYS[dayIndex].value, minute)));
}

watch(() => props.disabled, (disabled) => { if (disabled) endStroke(); });
onBeforeUnmount(() => endStroke());
</script>

<template>
	<div>
		<div class="grid-scroll border rounded p-2 bg-white">
			<div ref="root" class="availability-grid" :class="{ editable }" :aria-label="editable ? 'Your weekly availability' : 'Group weekly availability'" @pointermove="moveStroke" @pointerup="endStroke()" @pointercancel="endStroke(true)" @lostpointercapture="endStroke()">
				<div class="axis day-axis">Time</div>
				<div v-for="day in DAYS" :key="day.value" class="axis day-axis">{{ day.label }}</div>
				<template v-for="minute in GRID_TIMES" :key="minute">
					<div class="axis time-axis">{{ minuteLabel(minute) }}</div>
					<button v-for="(day, dayIndex) in DAYS" :key="`${day.value}-${minute}`" type="button" class="cell"
						:class="{ selected: editable && selected(day.value, minute) }"
						:style="{ backgroundColor: editable ? (selected(day.value, minute) ? 'var(--bs-primary)' : '#fff') : heatmapColor(cellAt(day.value, minute).freeCount, memberCount) }"
						:disabled="disabled" :tabindex="focused === `${dayIndex}-${minute}` ? 0 : -1"
						:aria-label="description(dayIndex, minute)" :aria-pressed="editable ? selected(day.value, minute) : undefined" :title="description(dayIndex, minute)"
						data-availability-cell :data-day-index="dayIndex" :data-minute="minute"
						@pointerdown="startStroke($event, dayIndex, minute)" @pointerenter="hovered = { dayIndex, minute }"
						@focus="hovered = { dayIndex, minute }; focused = `${dayIndex}-${minute}`" @keydown="keydown($event, dayIndex, minute)" @click="click($event, dayIndex, minute)">
						<span v-if="editable && selected(day.value, minute)" aria-hidden="true">✓</span>
					</button>
				</template>
			</div>
		</div>
		<div v-if="!editable" class="d-flex align-items-center gap-2 small text-muted mt-2"><span>0%</span><span class="legend-gradient"></span><span>100% of members available</span></div>
		<div v-if="hovered" class="border rounded p-2 mt-2 small" aria-live="polite">
			<strong>{{ blockLabel(hovered.dayIndex, hovered.minute) }}</strong>
			<template v-if="!editable"> · {{ hoverCell.freeCount }} of {{ memberCount }} available
				<div>{{ hoverCell.availableMembers.map((member) => member.name).join(', ') || 'No members available' }}</div>
			</template>
			<span v-else> · {{ selected(DAYS[hovered.dayIndex].value, hovered.minute) ? 'Available' : 'Unavailable' }}</span>
		</div>
	</div>
</template>

<style scoped>
.grid-scroll { max-height: 29rem; overflow: auto; }
.availability-grid { display: grid; grid-template-columns: 3.5rem repeat(7, minmax(2.7rem, 1fr)); gap: 3px; min-width: 430px; user-select: none; }
.axis { font-size: 0.75rem; text-align: center; align-self: center; background: white; color: var(--bs-secondary-color); }
.day-axis { position: sticky; top: -0.5rem; padding: 0.4rem 0; z-index: 2; }
.time-axis { position: sticky; left: 0; }
.cell { height: 1.6rem; border: 1px solid #e9ecef; border-radius: 4px; padding: 0; color: white; }
.editable .cell { touch-action: none; cursor: crosshair; }
.cell:focus-visible { outline: 3px solid #111; outline-offset: 1px; z-index: 1; }
.cell:disabled { cursor: wait; }
.legend-gradient { width: 6rem; height: 0.75rem; border: 1px solid #cbd0d8; background: linear-gradient(to right, white, var(--bs-primary)); }
</style>
