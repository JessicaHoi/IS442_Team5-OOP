<script setup>
import StudentAvatar from '../ui/StudentAvatar.vue';
import { programmeLine } from '../../utils/format';

defineProps({
	members: { type: Array, required: true },
	/** Hide the remove buttons (e.g. when the group is closed). */
	readonly: Boolean,
});
defineEmits(['remove']);
</script>

<template>
	<ul class="list-group list-group-flush">
		<li v-for="member in members" :key="member.id" class="list-group-item px-0 d-flex align-items-center gap-3">
			<StudentAvatar :name="member.name" />
			<div class="flex-grow-1">
				<div class="fw-medium">
					{{ member.name }}
					<span v-if="member.isLeader" class="badge badge-soft-primary ms-1">Leader</span>
				</div>
				<div class="small text-muted">{{ programmeLine(member) }}</div>
			</div>
			<button
				v-if="!member.isLeader && !readonly"
				type="button"
				class="btn btn-sm btn-outline-danger"
				@click="$emit('remove', member)"
			>
				Remove
			</button>
		</li>
	</ul>
</template>
