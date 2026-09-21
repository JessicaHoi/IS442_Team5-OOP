<script setup>
import ToastHost from './components/ui/ToastHost.vue';
import ConfirmModal from './components/ui/ConfirmModal.vue';
</script>

<template>
	<RouterView />
	<ToastHost />
	<ConfirmModal />
</template>
