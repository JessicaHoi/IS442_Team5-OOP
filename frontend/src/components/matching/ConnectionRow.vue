<script setup>
import StudentAvatar from '../ui/StudentAvatar.vue';
import ContactField from '../ui/ContactField.vue';
import { programmeLine, timeAgo } from '../../utils/format';

defineProps({
	/** Connection from GET /api/connections. */
	connection: { type: Object, required: true },
	/** Which tab the row is shown in: changes the available actions. */
	mode: { type: String, required: true, validator: (value) => ['incoming', 'sent', 'active'].includes(value) },
	busy: Boolean,
});
defineEmits(['accept', 'decline', 'end']);
</script>

<template>
	<li class="list-group-item px-3 px-md-4 py-3 d-flex flex-wrap align-items-center gap-3">
		<StudentAvatar :name="connection.other.name" />

		<div class="flex-grow-1">
			<div class="fw-medium">{{ connection.other.name }}</div>
			<div class="small text-muted">{{ programmeLine(connection.other) }} · {{ timeAgo(connection.createdAt) }}</div>
			<p v-if="connection.message && mode !== 'active'" class="small fst-italic mb-0 mt-1">“{{ connection.message }}”</p>
			<div v-if="mode === 'active'" class="small mt-1"><ContactField :value="connection.other.contactNumber" /></div>
		</div>

		<div class="d-flex gap-2 align-items-center">
			<template v-if="mode === 'incoming'">
				<button type="button" class="btn btn-sm btn-light border" :disabled="busy" @click="$emit('decline', connection)">Decline</button>
				<button type="button" class="btn btn-sm btn-primary" :disabled="busy" @click="$emit('accept', connection)">Accept</button>
			</template>
			<span v-else-if="mode === 'sent'" class="small text-muted"><i class="bi bi-hourglass-split me-1"></i>Awaiting response</span>
			<button v-else type="button" class="btn btn-sm btn-outline-danger" :disabled="busy" @click="$emit('end', connection)">End connection</button>
		</div>
	</li>
</template>
