<script setup>
/**
 * Bootstrap modal controlled with v-model. Pass `as-form` to wrap the body and
 * footer in a <form> so a submit button in the footer works.
 */
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import Modal from 'bootstrap/js/dist/modal';

const props = defineProps({
	modelValue: Boolean,
	title: { type: String, default: '' },
	size: { type: String, default: '' },
	asForm: Boolean,
});
const emit = defineEmits(['update:modelValue', 'submit', 'hidden']);

const root = ref(null);
let instance = null;

onMounted(() => {
	instance = new Modal(root.value);
	root.value.addEventListener('hidden.bs.modal', () => {
		emit('update:modelValue', false);
		emit('hidden');
	});
	if (props.modelValue) instance.show();
});

watch(
	() => props.modelValue,
	(open) => (open ? instance?.show() : instance?.hide()),
);

onBeforeUnmount(() => {
	// Leaving the page while open would otherwise leave a stray backdrop behind.
	const wasOpen = root.value?.classList.contains('show');
	instance?.dispose();
	if (wasOpen) {
		document.body.classList.remove('modal-open');
		document.body.style.removeProperty('overflow');
		document.body.style.removeProperty('padding-right');
		document.querySelectorAll('.modal-backdrop').forEach((backdrop) => backdrop.remove());
	}
});
</script>

<template>
	<Teleport to="body">
		<div ref="root" class="modal fade" tabindex="-1" aria-hidden="true">
			<div class="modal-dialog modal-dialog-centered modal-dialog-scrollable" :class="size && `modal-${size}`">
				<component :is="asForm ? 'form' : 'div'" class="modal-content" novalidate @submit.prevent="emit('submit')">
					<div class="modal-header border-0 pb-0">
						<h2 class="modal-title h5">{{ title }}</h2>
						<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
					</div>
					<div class="modal-body">
						<slot />
					</div>
					<div v-if="$slots.footer" class="modal-footer border-0 pt-0">
						<slot name="footer" />
					</div>
				</component>
			</div>
		</div>
	</Teleport>
</template>
