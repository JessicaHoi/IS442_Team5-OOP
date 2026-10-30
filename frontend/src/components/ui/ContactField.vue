<script setup>
import { telegramProfileUrl } from '../../utils/telegram';

defineProps({
	/** The contact number, or null while it is still hidden. */
	value: { type: String, default: null },
	/** The Telegram username, or null if hidden or not set. */
	telegramHandle: { type: String, default: null },
});
</script>

<template>
	<span v-if="value || telegramHandle" class="d-inline-flex flex-wrap align-items-center gap-2 fw-medium">
		<a v-if="telegramHandle" class="btn btn-sm btn-primary" :href="telegramProfileUrl(telegramHandle)" target="_blank" rel="noopener noreferrer">
			<i class="bi bi-telegram me-1"></i>Message on Telegram
		</a>
		<span v-if="value" class="d-inline-flex align-items-center gap-2"><i class="bi bi-telephone text-success"></i>{{ value }}</span>
	</span>
	<span v-else class="d-inline-flex align-items-center gap-2 text-muted">
		<i class="bi bi-lock"></i>Hidden until you are connected
	</span>
</template>
