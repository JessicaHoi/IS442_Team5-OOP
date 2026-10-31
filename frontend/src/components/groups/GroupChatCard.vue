<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { groupsApi } from '../../api/groups.api';
import { useAsync } from '../../composables/useAsync';
import { useConfirm } from '../../composables/useConfirm';
import { useToast } from '../../composables/useToast';
import { CHAT_MEMBER_STATES, CHAT_RIGHTS } from '../../utils/constants';
import { startPolling } from '../../utils/poll';
import LoadingState from '../ui/LoadingState.vue';
import ErrorState from '../ui/ErrorState.vue';

/** The group's Telegram chat: leaders set it up and see who's in, members get their invite. */
const props = defineProps({
	groupId: { type: String, required: true },
	canManage: { type: Boolean, default: false },
	groupOpen: { type: Boolean, default: true },
	/** The manually pasted link, offered while no chat is linked. */
	chatLink: { type: String, default: null },
});

const POLL_INTERVAL_MS = 3000;
const POLL_LIMIT_MS = 5 * 60 * 1000;

const { confirm } = useConfirm();
const toast = useToast();

const chat = ref(null);
const setupUrl = ref('');
const busy = ref(false);
let stopPolling = () => {};

const status = computed(() => chat.value?.status);
const me = computed(() => chat.value?.me);
const missingRights = computed(() => (chat.value?.missingRights ?? []).map((name) => CHAT_RIGHTS[name] ?? name));
const removalFailures = computed(() => (chat.value?.members ?? []).filter((member) => member.state === 'REMOVAL_FAILED'));

/** Loads the chat and, for a member who hasn't an invite yet, fetches one so the button is ready to tap. */
const { loading, error, run: load } = useAsync(async () => {
	chat.value = await groupsApi.chat(props.groupId);
	if (status.value === 'ACTIVE' && props.groupOpen && me.value?.state === 'NOT_INVITED') {
		chat.value = await groupsApi.chatInvite(props.groupId);
	}
});

watch(() => props.groupId, load, { immediate: true });
onBeforeUnmount(() => stopPolling());

/** Runs an action on the card and shows its error, if any, as a toast. */
async function act(action, successMessage) {
	busy.value = true;
	try {
		await action();
		if (successMessage) toast.success(successMessage);
	} catch (caught) {
		toast.error(caught.message);
	} finally {
		busy.value = false;
	}
}

function pollUntilLinked() {
	stopPolling();
	stopPolling = startPolling({
		intervalMs: POLL_INTERVAL_MS,
		maxMs: POLL_LIMIT_MS,
		isVisible: () => document.visibilityState === 'visible',
		check: async () => {
			const latest = await groupsApi.chat(props.groupId);
			if (latest.status === 'NOT_LINKED' || latest.status === 'DETACHED') return false;
			chat.value = latest;
			setupUrl.value = '';
			return latest.status === 'ACTIVE';
		},
		onTimeout: () => { setupUrl.value = ''; },
	});
}

const startSetup = () => act(async () => {
	setupUrl.value = (await groupsApi.startChatSetup(props.groupId)).url;
	pollUntilLinked();
});

const verify = () => act(async () => { chat.value = await groupsApi.verifyChat(props.groupId); });

const getInvite = (renew) => act(async () => { chat.value = await groupsApi.chatInvite(props.groupId, renew); });

async function notMe() {
	const confirmed = await confirm({
		title: 'Remove this Telegram account from the chat?',
		message: `${me.value.telegramName ?? 'That account'} will be removed from the chat, and you'll get a new invite. Only do this if it isn't you.`,
		confirmLabel: 'Remove and get a new invite',
		danger: true,
	});
	if (confirmed) await getInvite(true);
}

async function unlink() {
	const confirmed = await confirm({
		title: 'Unlink this chat?',
		message: 'Invites stop working and the StudyBuddy bot leaves. The chat itself and its members stay on Telegram.',
		confirmLabel: 'Unlink chat',
		danger: true,
	});
	if (!confirmed) return;
	await act(async () => {
		await groupsApi.unlinkChat(props.groupId);
		chat.value = await groupsApi.chat(props.groupId);
	}, 'Chat unlinked.');
}
</script>

<template>
	<section class="py-3" aria-labelledby="group-chat-title">
		<h2 id="group-chat-title" class="h5 mb-3"><i class="bi bi-telegram me-2"></i>Group chat</h2>

		<LoadingState v-if="loading && !chat" />
		<ErrorState v-else-if="error" :message="error" @retry="load" />

		<template v-else-if="chat">
			<!-- No chat yet, or the bot was removed -->
			<template v-if="status === 'NOT_LINKED' || status === 'DETACHED'">
				<div v-if="status === 'DETACHED'" class="alert alert-warning small" role="status">The StudyBuddy bot was removed from the chat, so it's no longer linked.</div>
				<template v-if="canManage">
					<template v-if="setupUrl">
						<p class="small text-body-secondary mb-2">Open the bot in Telegram, press <strong>Start</strong>, then choose <strong>Create a new group</strong>. Any name works; the bot renames it. This page updates by itself. Use the Telegram app, because Telegram Web can't create groups for bots.</p>
						<a class="btn btn-primary" :href="setupUrl" target="_blank" rel="noopener"><i class="bi bi-telegram me-2"></i>Open Telegram</a>
						<button type="button" class="btn btn-link btn-sm" :disabled="busy" @click="startSetup">Get a new link</button>
					</template>
					<template v-else>
						<p class="small text-body-secondary mb-2">Create a Telegram chat for this group. Members join with one tap, and people who leave the group are removed automatically.</p>
						<button type="button" class="btn btn-primary" :disabled="busy" @click="startSetup"><i class="bi bi-telegram me-2"></i>Set up group chat</button>
					</template>
				</template>
				<template v-else>
					<p class="small text-body-secondary mb-2">Your leader hasn't set up a chat yet.</p>
					<a v-if="chatLink" class="btn btn-primary" :href="chatLink" target="_blank" rel="noopener noreferrer">Open group chat</a>
				</template>
			</template>

			<p v-else-if="status === 'MANAGED_BY_OTHER_BOT'" class="small text-body-secondary mb-0">This group's chat is managed by another StudyBuddy bot, so it can't be used from here.</p>

			<template v-else>
				<p v-if="status === 'CLOSED'" class="small text-body-secondary">This group is closed, so its chat is read-only.</p>

				<!-- The bot hasn't the rights it needs -->
				<div v-if="status === 'NEEDS_ATTENTION'" class="mb-3">
					<div v-if="canManage" class="alert alert-warning small" role="status">
						<p class="mb-2">The bot can't manage <strong>{{ chat.title }}</strong> yet. In the group, open <strong>Administrators</strong>, add the StudyBuddy bot as an admin, and turn on:</p>
						<ul class="mb-2"><li v-for="right in missingRights" :key="right">{{ right }}</li></ul>
						<button type="button" class="btn btn-sm btn-outline-primary" :disabled="busy" @click="verify">I've done this, check again</button>
					</div>
					<p v-else class="small text-body-secondary mb-0">Your leader is finishing setting up the chat.</p>
				</div>

				<!-- This student's way in -->
				<template v-if="status === 'ACTIVE' && me">
					<template v-if="me.state === 'INVITED'">
						<a class="btn btn-primary mb-2" :href="me.inviteUrl" target="_blank" rel="noopener"><i class="bi bi-telegram me-2"></i>Join the group chat</a>
						<p class="small text-body-secondary mb-1">Telegram shows <em>Request to join</em>, and you're usually approved instantly. Other members will see your Telegram profile, and the StudyBuddy bot can see chat messages but doesn't store them.</p>
						<button type="button" class="btn btn-link btn-sm p-0" :disabled="busy" @click="getInvite(true)">Link not working? Get a new one</button>
					</template>
					<template v-else-if="me.state === 'JOINED'">
						<p class="small mb-1"><i class="bi bi-check-circle-fill text-success me-1"></i>You're in the chat{{ me.telegramName ? ` as ${me.telegramName}` : '' }}.</p>
						<button v-if="!canManage && me.telegramName" type="button" class="btn btn-link btn-sm p-0" :disabled="busy" @click="notMe">Not you?</button>
					</template>
					<template v-else-if="me.state === 'LEFT'">
						<p class="small text-body-secondary mb-2">You left the chat.</p>
						<button type="button" class="btn btn-outline-primary btn-sm" :disabled="busy" @click="getInvite(true)">Rejoin</button>
					</template>
				</template>

				<!-- What only the leader sees -->
				<template v-if="canManage">
					<div v-if="chat.ownedByFormerLeader" class="alert alert-info small mt-3" role="status">Telegram doesn't move chat ownership automatically. Ask the previous leader to make you an admin of the chat.</div>
					<div v-if="removalFailures.length" class="alert alert-warning small mt-3" role="status">
						Telegram wouldn't let the bot remove {{ removalFailures.map((member) => member.name).join(', ') }}, usually because they own or administer the chat. Remove them in Telegram yourself.
					</div>
					<ul v-if="chat.members?.length" class="list-unstyled small mt-3 mb-3">
						<li v-for="member in chat.members" :key="member.studentId" class="d-flex justify-content-between gap-3 py-1 border-bottom">
							<span>{{ member.name }}<span v-if="member.telegramName" class="text-body-secondary"> · {{ member.telegramName }}</span></span>
							<span class="text-body-secondary text-nowrap">{{ CHAT_MEMBER_STATES[member.state] ?? member.state }}</span>
						</li>
					</ul>
					<button v-if="status !== 'CLOSED'" type="button" class="btn btn-sm btn-outline-danger" :disabled="busy" @click="unlink">Unlink chat</button>
				</template>
			</template>
		</template>
	</section>
</template>
