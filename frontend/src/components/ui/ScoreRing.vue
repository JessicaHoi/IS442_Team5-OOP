<script setup>
import { computed } from 'vue';

const props = defineProps({
	value: { type: Number, required: true },
	size: { type: Number, default: 60 },
});

const STROKE = 5;
const radius = computed(() => (props.size - STROKE) / 2);
const circumference = computed(() => 2 * Math.PI * radius.value);
const dashOffset = computed(() => circumference.value * (1 - Math.min(100, Math.max(0, props.value)) / 100));

const tone = computed(() => {
	if (props.value >= 75) return 'var(--bs-success)';
	if (props.value >= 50) return 'var(--bs-primary)';
	if (props.value >= 25) return 'var(--bs-warning)';
	return 'var(--bs-secondary)';
});
</script>

<template>
	<div
		class="position-relative d-inline-block flex-shrink-0"
		:style="{ width: `${size}px`, height: `${size}px` }"
		role="img"
		:aria-label="`Match score ${value} out of 100`"
	>
		<svg :width="size" :height="size" class="ring">
			<circle :cx="size / 2" :cy="size / 2" :r="radius" fill="none" stroke="#e8eaf1" :stroke-width="STROKE" />
			<circle
				:cx="size / 2"
				:cy="size / 2"
				:r="radius"
				fill="none"
				:stroke="tone"
				:stroke-width="STROKE"
				stroke-linecap="round"
				:stroke-dasharray="circumference"
				:stroke-dashoffset="dashOffset"
			/>
		</svg>
		<span class="position-absolute top-50 start-50 translate-middle fw-semibold value" :style="{ fontSize: `${size * 0.28}px` }">
			{{ value }}<small class="text-muted fw-normal">%</small>
		</span>
	</div>
</template>

<style scoped>
.ring {
	transform: rotate(-90deg);
}
.value small {
	font-size: 0.6em;
}
</style>
