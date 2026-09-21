<script setup>
import { computed } from 'vue';
import { formatSlot, sortSlots } from '../../utils/availability';

const props = defineProps({
	slots: { type: Array, default: () => [] },
	emptyText: { type: String, default: 'No availability set' },
});

const sorted = computed(() => sortSlots(props.slots));
</script>

<template>
	<div class="d-flex flex-wrap gap-1">
		<span v-for="slot in sorted" :key="`${slot.day}-${slot.start}`" class="chip-static">
			<i class="bi bi-clock"></i>{{ formatSlot(slot) }}
		</span>
		<span v-if="sorted.length === 0" class="text-muted small">{{ emptyText }}</span>
	</div>
</template>
