<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { integrationsApi } from '../../api/integrations.api';
import { studentsApi } from '../../api/students.api';
import { useConfirm } from '../../composables/useConfirm';
import { useToast } from '../../composables/useToast';
import { startPolling } from '../../utils/poll';

const POLL_INTERVAL_MS = 3000;
const POLL_LIMIT_MS = 2 * 60 * 1000;

const { confirm } = useConfirm();
const toast = useToast();

/** Stays false when Telegram is off or can't be checked, which hides this card: the profile works without it. */
const enabled = ref(false);
const status = ref(null);
const linkUrl = ref('');
const timedOut = ref(false);
const busy = ref(false);
let stopPolling = () => {};

const accountName = computed(() => (status.value.username ? `@${status.value.username}` : status.value.firstName));

onMounted(async () => {
	try {
		const { telegram } = await integrationsApi.get();
		if (!telegram.enabled) return;
		status.value = await studentsApi.telegramStatus();
		enabled.value = true;
	} catch {
		enabled.value = false;
	}
});

onBeforeUnmount(() => stopPolling());

async function checkLinked() {
	const latest = await studentsApi.telegramStatus();
	if (!latest.linked) return false;
	status.value = latest;
	linkUrl.value = '';
	toast.success('Telegram connected.');
	return true;
}

async function connect() {
	stopPolling();
	busy.value = true;
	timedOut.value = false;
	try {
		linkUrl.value = (await studentsApi.createTelegramLink()).url;
		stopPolling = startPolling({
			check: checkLinked,
			intervalMs: POLL_INTERVAL_MS,
			maxMs: POLL_LIMIT_MS,
			isVisible: () => document.visibilityState === 'visible',
			onTimeout: () => {
				timedOut.value = true;
			},
		});
	} catch (caught) {
		toast.error(caught.message);
	} finally {
		busy.value = false;
	}
}

async function disconnect() {
	const confirmed = await confirm({
		title: 'Disconnect Telegram?',
		message: 'You can connect it again at any time.',
		confirmLabel: 'Disconnect',
		danger: true,
	});
	if (!confirmed) return;
	busy.value = true;
	try {
		await studentsApi.unlinkTelegram();
		status.value = await studentsApi.telegramStatus();
		toast.success('Telegram disconnected.');
	} catch (caught) {
		toast.error(caught.message);
	} finally {
		busy.value = false;
	}
}
</script>

<template>
	<section v-if="enabled" class="card-flat p-4 mb-4">
		<h2 class="h6 mb-1">Telegram</h2>

		<div v-if="status.linked" class="d-flex flex-wrap align-items-center justify-content-between gap-2">
			<span class="small"><i class="bi bi-check-circle-fill text-success me-1"></i>Connected as {{ accountName }}</span>
			<button type="button" class="btn btn-sm btn-outline-secondary" :disabled="busy" @click="disconnect">Disconnect</button>
		</div>

		<template v-else-if="linkUrl && !timedOut">
			<p class="text-muted small mb-2">Open Telegram and tap <strong>Start</strong>. We'll update this page when you're connected.</p>
			<a class="btn btn-primary btn-sm" :href="linkUrl" target="_blank" rel="noopener">
				<i class="bi bi-telegram me-1"></i>Open Telegram
			</a>
		</template>

		<template v-else>
			<p class="text-muted small mb-2">
				{{ timedOut ? "We didn't hear from Telegram. Get a new link and try again." : 'Connect your account to answer sessions from your group\'s chat.' }}
			</p>
			<button type="button" class="btn btn-outline-primary btn-sm" :disabled="busy" @click="connect">
				<i class="bi bi-telegram me-1"></i>{{ timedOut ? 'Get a new link' : 'Connect Telegram' }}
			</button>
		</template>
	</section>
</template>
