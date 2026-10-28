<script setup>
import { computed, reactive, ref, watch } from 'vue';
import AppModal from '../ui/AppModal.vue';
import FormField from '../ui/FormField.vue';
import { ROLES, USER_STATUSES } from '../../utils/constants';

const props = defineProps({
	modelValue: Boolean,
	/** Account being edited; null when creating. */
	user: { type: Object, default: null },
	isSelf: Boolean,
	saving: Boolean,
	/** Server-side error to show above the form. */
	error: { type: String, default: '' },
});
const emit = defineEmits(['update:modelValue', 'submit']);

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD_LENGTH = 6;

const isEditing = computed(() => props.user !== null);
const form = reactive({ name: '', email: '', role: ROLES.STUDENT, status: 'ACTIVE', password: '' });
const errors = ref({});

watch(
	() => props.modelValue,
	(open) => {
		if (!open) return;
		Object.assign(form, {
			name: props.user?.name ?? '',
			email: props.user?.email ?? '',
			role: props.user?.role ?? ROLES.STUDENT,
			status: props.user?.status ?? 'ACTIVE',
			password: '',
		});
		errors.value = {};
	},
);

function validate() {
	const found = {};
	if (!form.name.trim()) found.name = 'Name is required.';
	else if (form.name.trim().length > 100) found.name = 'Name must be at most 100 characters.';
	if (!EMAIL_PATTERN.test(form.email.trim())) found.email = 'Enter a valid email address.';
	else if (form.email.trim().length > 255) found.email = 'Email must be at most 255 characters.';
	const needsPassword = !isEditing.value || form.password.length > 0;
	if (needsPassword && form.password.length < MIN_PASSWORD_LENGTH) {
		found.password = `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`;
	}
	if (needsPassword && !form.password.trim()) found.password = 'Password must not be blank.';
	if (needsPassword && new TextEncoder().encode(form.password).length > 72) {
		found.password = 'Password is too long. Use fewer characters.';
	}
	errors.value = found;
	return Object.keys(found).length === 0;
}

function onSubmit() {
	if (props.saving) return;
	if (!validate()) return;
	const payload = { name: form.name.trim(), email: form.email.trim(), role: form.role, status: form.status };
	if (form.password) payload.password = form.password;
	emit('submit', payload);
}
</script>

<template>
	<AppModal
		:model-value="modelValue"
		:title="isEditing ? 'Edit account' : 'Create account'"
		as-form
		:busy="saving"
		@update:model-value="emit('update:modelValue', $event)"
		@submit="onSubmit"
	>
		<div v-if="error" class="alert alert-danger py-2">{{ error }}</div>

		<FormField label="Full name" required :error="errors.name" v-slot="{ id, invalidClass }">
			<input :id="id" v-model="form.name" class="form-control" :class="invalidClass" autocomplete="off" />
		</FormField>
		<FormField label="Email" required :error="errors.email" v-slot="{ id, invalidClass }">
			<input :id="id" v-model="form.email" type="email" class="form-control" :class="invalidClass" autocomplete="off" />
		</FormField>
		<div class="row">
			<div class="col-sm-6">
				<FormField label="Role" v-slot="{ id }">
					<select :id="id" v-model="form.role" class="form-select" :disabled="isEditing">
						<option :value="ROLES.STUDENT">Student</option>
						<option :value="ROLES.ADMIN">System administrator</option>
					</select>
				</FormField>
			</div>
			<div class="col-sm-6">
				<FormField label="Status" v-slot="{ id }">
					<select :id="id" v-model="form.status" class="form-select" :disabled="isSelf">
						<option v-for="status in USER_STATUSES" :key="status.value" :value="status.value">{{ status.label }}</option>
					</select>
				</FormField>
			</div>
		</div>
		<FormField
			:label="isEditing ? 'New password' : 'Password'"
			:required="!isEditing"
			:error="errors.password"
			:hint="isEditing ? 'Leave blank to keep the current password.' : ''"
			v-slot="{ id, invalidClass }"
		>
			<input :id="id" v-model="form.password" type="password" class="form-control" :class="invalidClass" autocomplete="new-password" />
		</FormField>

		<template #footer>
			<button type="button" class="btn btn-light" data-bs-dismiss="modal" :disabled="saving">Cancel</button>
			<button type="submit" class="btn btn-primary" :disabled="saving">
				<span v-if="saving" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
				{{ isEditing ? 'Save changes' : 'Create account' }}
			</button>
		</template>
	</AppModal>
</template>
