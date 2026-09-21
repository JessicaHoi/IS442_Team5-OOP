<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { connectionsApi } from '../../api/connections.api';
import { useToast } from '../../composables/useToast';
import { useConfirm } from '../../composables/useConfirm';
import { useAsync } from '../../composables/useAsync';
import PageHeader from '../../components/ui/PageHeader.vue';
import EmptyState from '../../components/ui/EmptyState.vue';
import LoadingState from '../../components/ui/LoadingState.vue';
import ErrorState from '../../components/ui/ErrorState.vue';
import ConnectionRow from '../../components/matching/ConnectionRow.vue';

const TABS = [
	{
		key: 'incoming',
		label: 'Requests',
		empty: { icon: 'bi-inbox', title: 'No pending requests', message: 'Requests from other students will appear here.' },
	},
	{
		key: 'sent',
		label: 'Sent',
		empty: { icon: 'bi-send', title: 'No sent requests', message: 'Requests you send and are waiting on will appear here.' },
	},
	{
		key: 'active',
		label: 'Buddies',
		empty: { icon: 'bi-people', title: 'No study buddies yet', message: 'Once a request is accepted, your buddy’s contact number appears here.' },
	},
];

const route = useRoute();
const toast = useToast();
const { confirm } = useConfirm();

const initialTab = TABS.some((tab) => tab.key === route.query.tab) ? route.query.tab : 'incoming';
const activeTab = ref(initialTab);
const lists = reactive({ incoming: [], sent: [], active: [] });
const busyId = ref(null);

const current = computed(() => TABS.find((tab) => tab.key === activeTab.value));

const { loading, error, run: loadAll } = useAsync(async () => {
	const [incoming, sent, active] = await Promise.all(TABS.map((tab) => connectionsApi.list(tab.key)));
	Object.assign(lists, { incoming, sent, active });
});

onMounted(loadAll);

/** Run a connection action, then refresh the lists. */
async function perform(connection, action, successMessage) {
	busyId.value = connection.id;
	try {
		await action();
		toast.success(successMessage);
		await loadAll();
	} catch (caught) {
		toast.error(caught.message);
	} finally {
		busyId.value = null;
	}
}

const accept = (connection) =>
	perform(connection, () => connectionsApi.accept(connection.id), `You are now connected with ${connection.other.name}.`);

const decline = (connection) =>
	perform(connection, () => connectionsApi.decline(connection.id), `Request from ${connection.other.name} declined.`);

async function end(connection) {
	const confirmed = await confirm({
		title: 'End this connection?',
		message: `You and ${connection.other.name} will no longer see each other’s contact details. You can send a new request later.`,
		confirmLabel: 'End connection',
		danger: true,
	});
	if (confirmed) {
		await perform(connection, () => connectionsApi.end(connection.id), `Connection with ${connection.other.name} ended.`);
	}
}
</script>

<template>
	<div>
		<PageHeader title="Connections" subtitle="Manage the study-buddy requests you have received and sent.">
			<template #actions>
				<RouterLink class="btn btn-primary" to="/matches"><i class="bi bi-search-heart me-2"></i>Find buddies</RouterLink>
			</template>
		</PageHeader>

		<ul class="nav nav-underline mb-4" role="tablist">
			<li v-for="tab in TABS" :key="tab.key" class="nav-item" role="presentation">
				<button
					type="button"
					class="nav-link"
					:class="{ active: activeTab === tab.key }"
					role="tab"
					:aria-selected="activeTab === tab.key"
					@click="activeTab = tab.key"
				>
					{{ tab.label }}
					<span class="badge rounded-pill ms-1" :class="activeTab === tab.key ? 'text-bg-primary' : 'text-bg-light border'">
						{{ lists[tab.key].length }}
					</span>
				</button>
			</li>
		</ul>

		<LoadingState v-if="loading && !lists[activeTab].length" />
		<ErrorState v-else-if="error" :message="error" @retry="loadAll" />
		<div v-else class="card-flat">
			<EmptyState v-if="lists[activeTab].length === 0" v-bind="current.empty" />
			<ul v-else class="list-group list-group-flush">
				<ConnectionRow
					v-for="connection in lists[activeTab]"
					:key="connection.id"
					:connection="connection"
					:mode="activeTab"
					:busy="busyId === connection.id"
					@accept="accept"
					@decline="decline"
					@end="end"
				/>
			</ul>
		</div>
	</div>
</template>
