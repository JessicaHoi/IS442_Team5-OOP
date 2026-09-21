<script setup>
import { computed } from 'vue';
import { initials } from '../../utils/format';

const props = defineProps({
	name: { type: String, default: '' },
	size: { type: Number, default: 44 },
});

const style = computed(() => ({
	width: `${props.size}px`,
	height: `${props.size}px`,
	fontSize: `${Math.round(props.size * 0.38)}px`,
}));
</script>

<template>
	<span class="avatar" :style="style" aria-hidden="true">{{ initials(name) }}</span>
</template>

<style scoped>
.avatar {
	display: inline-flex;
	flex-shrink: 0;
	align-items: center;
	justify-content: center;
	border-radius: 50%;
	background: rgba(79, 70, 229, 0.1);
	color: #4338ca;
	font-weight: 600;
	letter-spacing: 0.02em;
}
</style>
