<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { adminApi } from '../../api/admin.api';
import { useAuthStore } from '../../stores/auth';
import { useToast } from '../../composables/useToast';
import { useConfirm } from '../../composables/useConfirm';
import { useAsync } from '../../composables/useAsync';
import { ROLES } from '../../utils/constants';
import { timeAgo } from '../../utils/format';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusBadge from '../../components/ui/StatusBadge.vue';
import StudentAvatar from '../../components/ui/StudentAvatar.vue';
import LoadingState from '../../components/ui/LoadingState.vue';
import ErrorState from '../../components/ui/ErrorState.vue';
import EmptyState from '../../components/ui/EmptyState.vue';
import UserFormModal from '../../components/admin/UserFormModal.vue';

const PAGE_SIZE = 12;

const auth = useAuthStore();
const toast = useToast();
const { confirm } = useConfirm();

const isSelf = (user) => user.id === auth.user?.id;

const users = ref([]);
const search = ref('');
const page = ref(1);

const { loading, error, run: load } = useAsync(async () => {
	users.value = await adminApi.listUsers();
});
onMounted(load);

const summary = computed(() => [
	{ label: 'Total accounts', value: users.value.length, icon: 'bi-people' },
	{ label: 'Students', value: users.value.filter((u) => u.role === ROLES.STUDENT).length, icon: 'bi-mortarboard' },
	{ label: 'Active', value: users.value.filter((u) => u.status === 'ACTIVE').length, icon: 'bi-check-circle' },
	{ label: 'Suspended', value: users.value.filter((u) => u.status === 'SUSPENDED').length, icon: 'bi-slash-circle' },
]);

const filtered = computed(() => {
	const term = search.value.trim().toLowerCase();
	const matching = term
		? users.value.filter((u) => u.name.toLowerCase().includes(term) || u.email.toLowerCase().includes(term))
		: users.value;
	return [...matching].sort((a, b) => a.name.localeCompare(b.name));
});
const pageCount = computed(() => Math.max(1, Math.ceil(filtered.value.length / PAGE_SIZE)));
const visible = computed(() => filtered.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE));

watch(search, () => (page.value = 1));
watch(pageCount, (count) => page.value > count && (page.value = count));

// ---- Create / edit --------------------------------------------------------
const formOpen = ref(false);
const editing = ref(null);
const saving = ref(false);
const formError = ref('');

function openCreate() {
	editing.value = null;
	formError.value = '';
	formOpen.value = true;
}

function openEdit(user) {
	editing.value = user;
	formError.value = '';
	formOpen.value = true;
}

async function saveUser(payload) {
	saving.value = true;
	formError.value = '';
	try {
		if (editing.value) await adminApi.updateUser(editing.value.id, payload);
		else await adminApi.createUser(payload);
		formOpen.value = false;
		toast.success(editing.value ? 'Account updated.' : 'Account created.');
		await load();
	} catch (caught) {
		formError.value = caught.message;
	} finally {
		saving.value = false;
	}
}

// ---- Delete ---------------------------------------------------------------
async function deleteUser(user) {
	const confirmed = await confirm({
		title: `Delete ${user.name}?`,
		message: 'This permanently removes the account, their connections, join requests and any groups they lead.',
		confirmLabel: 'Delete account',
		danger: true,
	});
	if (!confirmed) return;
	try {
		await adminApi.deleteUser(user.id);
		toast.success('Account deleted.');
		await load();
	} catch (caught) {
		toast.error(caught.message);
	}
}
</script>

<template>
	<div>
		<PageHeader title="User accounts" subtitle="Create, update and remove accounts, and see how they are being used.">
			<template #actions>
				<button type="button" class="btn btn-primary" @click="openCreate"><i class="bi bi-plus-lg me-2"></i>Create account</button>
			</template>
		</PageHeader>

		<LoadingState v-if="loading && users.length === 0" />
		<ErrorState v-else-if="error" :message="error" @retry="load" />
		<template v-else>
			<div class="row g-3 mb-4">
				<div v-for="item in summary" :key="item.label" class="col-6 col-lg-3">
					<div class="card-flat p-3 d-flex align-items-center gap-3">
						<span class="stat-icon"><i class="bi" :class="item.icon"></i></span>
						<div>
							<div class="h4 mb-0">{{ item.value }}</div>
							<div class="small text-muted">{{ item.label }}</div>
						</div>
					</div>
				</div>
			</div>

			<div class="card-flat">
				<div class="p-3 border-bottom">
					<div class="input-group search-box">
						<span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
						<input v-model="search" type="search" class="form-control border-start-0" placeholder="Search by name or email" aria-label="Search accounts" />
					</div>
				</div>

				<EmptyState v-if="filtered.length === 0" icon="bi-search" title="No accounts found" message="Try a different search term." />
				<div v-else class="table-responsive">
					<table class="table align-middle mb-0">
						<thead>
							<tr class="small text-muted">
								<th class="ps-3">Account</th>
								<th>Role</th>
								<th>Status</th>
								<th>Last active</th>
								<th class="text-end">Connections</th>
								<th class="text-end">Groups</th>
								<th class="pe-3 text-end"><span class="visually-hidden">Actions</span></th>
							</tr>
						</thead>
						<tbody>
							<tr v-for="user in visible" :key="user.id">
								<td class="ps-3">
									<div class="d-flex align-items-center gap-2">
										<StudentAvatar :name="user.name" :size="34" />
										<div>
											<div class="fw-medium">{{ user.name }}</div>
											<div class="small text-muted">{{ user.email }}</div>
										</div>
									</div>
								</td>
								<td>{{ user.role === ROLES.ADMIN ? 'Administrator' : 'Student' }}</td>
								<td><StatusBadge :status="user.status" /></td>
								<td class="text-muted">{{ timeAgo(user.lastLogin) }}</td>
								<td class="text-end">{{ user.role === ROLES.STUDENT ? user.connectionCount : '–' }}</td>
								<td class="text-end">{{ user.role === ROLES.STUDENT ? user.groupCount : '–' }}</td>
								<td class="pe-3 text-end text-nowrap">
									<button type="button" class="btn btn-icon btn-light border me-1" :aria-label="`Edit ${user.name}`" title="Edit" @click="openEdit(user)">
										<i class="bi bi-pencil"></i>
									</button>
									<button
										type="button"
										class="btn btn-icon btn-light border text-danger"
										:aria-label="`Delete ${user.name}`"
										:title="isSelf(user) ? 'You cannot delete your own account' : 'Delete'"
										:disabled="isSelf(user)"
										@click="deleteUser(user)"
									>
										<i class="bi bi-trash"></i>
									</button>
								</td>
							</tr>
						</tbody>
					</table>
				</div>

				<div v-if="pageCount > 1" class="d-flex align-items-center justify-content-between p-3 border-top">
					<span class="small text-muted">{{ filtered.length }} accounts</span>
					<div class="d-flex align-items-center gap-2">
						<button type="button" class="btn btn-sm btn-light border" :disabled="page === 1" @click="page--"><i class="bi bi-chevron-left"></i> Prev</button>
						<span class="small">Page {{ page }} of {{ pageCount }}</span>
						<button type="button" class="btn btn-sm btn-light border" :disabled="page === pageCount" @click="page++">Next <i class="bi bi-chevron-right"></i></button>
					</div>
				</div>
			</div>
		</template>

		<UserFormModal v-model="formOpen" :user="editing" :saving="saving" :error="formError" @submit="saveUser" />
	</div>
</template>

<style scoped>
.stat-icon {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	width: 2.5rem;
	height: 2.5rem;
	border-radius: 0.6rem;
	background: rgba(79, 70, 229, 0.08);
	color: var(--bs-primary);
	font-size: 1.15rem;
}
.search-box {
	max-width: 22rem;
}
</style>
