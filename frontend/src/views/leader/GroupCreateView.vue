<script setup>
import { onMounted, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { groupsApi } from '../../api/groups.api';
import { useLookupsStore } from '../../stores/lookups';
import { useToast } from '../../composables/useToast';
import { useAsync } from '../../composables/useAsync';
import PageHeader from '../../components/ui/PageHeader.vue';
import LoadingState from '../../components/ui/LoadingState.vue';
import ErrorState from '../../components/ui/ErrorState.vue';
import GroupForm from '../../components/groups/GroupForm.vue';

const lookups = useLookupsStore();
const router = useRouter();
const toast = useToast();
const saving = ref(false);

const { loading, error, run: load } = useAsync(() => lookups.loadCourses());
onMounted(load);

async function create(payload) {
	saving.value = true;
	try {
		const group = await groupsApi.create(payload);
		toast.success('Study group created. You are now its leader.');
		await router.replace({ name: 'group-manage', params: { id: group.id } });
	} catch (caught) {
		toast.error(caught.message);
	} finally {
		saving.value = false;
	}
}
</script>

<template>
	<div>
		<RouterLink to="/groups" class="text-decoration-none small"><i class="bi bi-arrow-left me-1"></i>Study groups</RouterLink>
		<PageHeader class="mt-2" title="Create a study group" subtitle="You will be the group leader and can manage requests and members." />

		<LoadingState v-if="loading" />
		<ErrorState v-else-if="error" :message="error" @retry="load" />
		<div v-else class="card-flat p-4">
			<GroupForm :saving="saving" submit-label="Create group" @submit="create" />
		</div>
	</div>
</template>
