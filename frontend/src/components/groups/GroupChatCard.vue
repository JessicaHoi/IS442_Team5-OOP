<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { groupsApi } from '../../api/groups.api';
import { useAsync } from '../../composables/useAsync';
import { useConfirm } from '../../composables/useConfirm';
import { useToast } from '../../composables/useToast';
import { CHAT_MEMBER_STATES, CHAT_RIGHTS } from '../../utils/constants';
import { startPolling } from '../../utils/poll';
import { needsInvite } from '../../utils/telegram';
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

const CHAT_SUMMARY = 'See who\'s coming, answer from the chat, and get a reminder before each session.';
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

/** Loads the chat and, for a member without a working invite, fetches one so the button is ready to tap. */
const { loading, error, run: load } = useAsync(async () => {
	chat.value = await groupsApi.chat(props.groupId);
	if (status.value === 'ACTIVE' && props.groupOpen && needsInvite(me.value)) {
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

/** "Not you?" can remove someone and then fail to make a new invite, so a failure reloads to show where things stand. */
const getInvite = (renew) => act(async () => {
	try {
		chat.value = await groupsApi.chatInvite(props.groupId, renew);
	} catch (caught) {
		chat.value = await groupsApi.chat(props.groupId).catch(() => chat.value);
		throw caught;
	}
});

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
		message: 'Invite links will stop working and the bot will leave. The chat and its members stay on Telegram.',
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
				<div v-if="status === 'DETACHED'" class="alert alert-warning small" role="status">This chat is no longer linked because the StudyBuddy bot was removed.</div>
				<template v-if="canManage">
					<template v-if="setupUrl">
						<ol class="small text-body-secondary ps-3 mb-2">
							<li>Open the Telegram app and tap <strong>Start</strong>. (It won't work in Telegram Web.)</li>
							<li>Tap <strong>Create a new group</strong>. Any name is fine, we'll rename it for you.</li>
						</ol>
						<p class="small text-body-secondary mb-2">We'll update this page when it's ready.</p>
						<a class="btn btn-primary" :href="setupUrl" target="_blank" rel="noopener"><i class="bi bi-telegram me-2"></i>Open Telegram</a>
						<button type="button" class="btn btn-link btn-sm" :disabled="busy" @click="startSetup">Get a new link</button>
					</template>
					<template v-else>
						<p class="small text-body-secondary mb-3">Members join with one tap, and leave the chat when they leave the group. {{ CHAT_SUMMARY }}</p>
						<button type="button" class="btn btn-primary" :disabled="busy" @click="startSetup"><i class="bi bi-telegram me-2"></i>Set up group chat</button>
					</template>
				</template>
				<template v-else>
					<p class="small text-body-secondary mb-2">Your leader hasn't set up a chat yet.</p>
					<a v-if="chatLink" class="btn btn-primary" :href="chatLink" target="_blank" rel="noopener noreferrer">Open group chat</a>
				</template>
			</template>

			<p v-else-if="status === 'MANAGED_BY_OTHER_BOT'" class="small text-body-secondary mb-0">This group's chat is linked to another StudyBuddy bot, so you can't use it here.</p>

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

				<p v-if="status === 'ACTIVE'" class="small text-body-secondary mb-3">{{ CHAT_SUMMARY }}</p>

				<!-- This student's way in -->
				<template v-if="status === 'ACTIVE' && me">
					<template v-if="me.state === 'INVITED'">
						<a class="btn btn-primary mb-2" :href="me.inviteUrl" target="_blank" rel="noopener"><i class="bi bi-telegram me-2"></i>Join the group chat</a>
						<p class="small text-body-secondary mb-1">Tap <em>Request to join</em> in Telegram. Other members will see your Telegram profile. StudyBuddy can see chat messages but doesn't save them.</p>
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
					<template v-else-if="me.state === 'REMOVED' || me.state === 'NOT_INVITED'">
						<p class="small text-body-secondary mb-2">You're not in the chat yet.</p>
						<button type="button" class="btn btn-outline-primary btn-sm" :disabled="busy" @click="getInvite(false)">Get an invite</button>
					</template>
				</template>

				<!-- What only the leader sees -->
				<template v-if="canManage">
					<div v-if="chat.ownedByFormerLeader" class="alert alert-info small mt-3" role="status">Ask the previous leader to make you an admin of the chat in Telegram.</div>
					<div v-if="removalFailures.length" class="alert alert-warning small mt-3" role="status">
						We couldn't remove {{ removalFailures.map((member) => member.name).join(', ') }} from the chat, usually because they're an admin. You'll need to remove them in Telegram.
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
