<script setup>
import { computed } from 'vue';

const props = defineProps({ status: { type: String, required: true } });

const STATUSES = {
	ACTIVE: { label: 'Active', tone: 'success' },
	SUSPENDED: { label: 'Suspended', tone: 'danger' },
	OPEN: { label: 'Open', tone: 'success' },
	CLOSED: { label: 'Closed', tone: 'secondary' },
	PENDING: { label: 'Pending', tone: 'warning' },
	PENDING_SENT: { label: 'Request sent', tone: 'warning' },
	PENDING_RECEIVED: { label: 'Wants to connect', tone: 'primary' },
	CONNECTED: { label: 'Connected', tone: 'success' },
	LEADER: { label: 'You lead this group', tone: 'primary' },
	MEMBER: { label: 'Member', tone: 'success' },
};

const config = computed(() => STATUSES[props.status] ?? { label: props.status, tone: 'secondary' });
</script>

<template>
	<span class="badge" :class="`badge-soft-${config.tone}`">{{ config.label }}</span>
</template>
