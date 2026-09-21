<script setup>
import StudentAvatar from '../ui/StudentAvatar.vue';
import EmptyState from '../ui/EmptyState.vue';
import { programmeLine, timeAgo } from '../../utils/format';

defineProps({
	requests: { type: Array, required: true },
	/** Id of the request currently being processed. */
	busyId: { type: Number, default: null },
});
defineEmits(['accept', 'reject']);
</script>

<template>
	<EmptyState v-if="requests.length === 0" icon="bi-person-check" title="No pending requests" message="When students ask to join this group, they will appear here." />
	<ul v-else class="list-group list-group-flush">
		<li v-for="request in requests" :key="request.id" class="list-group-item px-0 d-flex flex-wrap align-items-center gap-3">
			<StudentAvatar :name="request.student.name" />
			<div class="flex-grow-1">
				<div class="fw-medium">{{ request.student.name }}</div>
				<div class="small text-muted">{{ programmeLine(request.student) }} · {{ timeAgo(request.createdAt) }}</div>
				<p v-if="request.message" class="small fst-italic mb-0 mt-1">“{{ request.message }}”</p>
			</div>
			<div class="d-flex gap-2">
				<button type="button" class="btn btn-sm btn-light border" :disabled="busyId === request.id" @click="$emit('reject', request)">Reject</button>
				<button type="button" class="btn btn-sm btn-primary" :disabled="busyId === request.id" @click="$emit('accept', request)">Accept</button>
			</div>
		</li>
	</ul>
</template>
