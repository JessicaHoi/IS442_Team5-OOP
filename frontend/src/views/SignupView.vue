<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { useLookupsStore } from '../stores/lookups';
import { SCHOOLS, YEARS_OF_STUDY } from '../utils/constants';
import { INVALID_HANDLE_MESSAGE, normalizeTelegramHandle } from '../utils/telegram';
import { courseInputError, passwordError, PHONE_PATTERN } from '../utils/validation';
import FormField from '../components/ui/FormField.vue';
import ChipSelect from '../components/ui/ChipSelect.vue';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const auth = useAuthStore();
const lookups = useLookupsStore();
const route = useRoute();
const router = useRouter();

const form = reactive({
	name: '',
	email: '',
	password: '',
	confirmPassword: '',
	school: '',
	programme: '',
	yearOfStudy: '',
	contactNumber: '',
	telegramHandle: '',
	courses: [],
});
const errors = ref({});
const error = ref('');
const courseError = ref('');
const newCourseCode = ref('');
const newCourseName = ref('');
const newCourses = ref([]);
const newCourseError = ref('');
const loadingCourses = ref(false);
const busy = ref(false);
const courseOptions = computed(() =>
	[...lookups.courses, ...newCourses.value].map((course) => ({ value: course.code, label: course.code, title: course.name })),
);

async function loadCourses() {
	loadingCourses.value = true;
	courseError.value = '';
	try {
		await lookups.loadCourses();
	} catch (caught) {
		courseError.value = caught.message;
	} finally {
		loadingCourses.value = false;
	}
}

onMounted(loadCourses);

function addCourse() {
	newCourseError.value = '';
	const code = newCourseCode.value.trim().toUpperCase();
	const name = newCourseName.value.trim();
	newCourseError.value = courseInputError(code, name) ?? '';
	if (newCourseError.value) return;
	if ([...lookups.courses, ...newCourses.value].some((course) => course.code === code)) {
		newCourseError.value = 'That course code is already listed. Select it above.';
		return;
	}
	newCourses.value = [...newCourses.value, { code, name }];
	form.courses = [...form.courses, code];
	newCourseCode.value = '';
	newCourseName.value = '';
}

function validate() {
	const found = {};
	if (!form.name.trim() || form.name.trim().length > 100) found.name = 'Enter a name of at most 100 characters.';
	if (!EMAIL_PATTERN.test(form.email.trim()) || form.email.trim().length > 255) found.email = 'Enter a valid email address.';
	const invalidPassword = passwordError(form.password);
	if (invalidPassword) found.password = invalidPassword;
	if (form.confirmPassword !== form.password) found.confirmPassword = 'Passwords do not match.';
	if (!form.school) found.school = 'Choose your school.';
	if (!form.programme.trim() || form.programme.trim().length > 100) found.programme = 'Enter a programme of at most 100 characters.';
	if (!YEARS_OF_STUDY.includes(Number(form.yearOfStudy))) found.yearOfStudy = 'Choose your year of study.';
	if (!PHONE_PATTERN.test(form.contactNumber.trim())) found.contactNumber = 'Enter a valid phone number, e.g. +65 9123 4567.';
	if (normalizeTelegramHandle(form.telegramHandle) === null) found.telegramHandle = INVALID_HANDLE_MESSAGE;
	if (form.courses.length === 0) found.courses = 'Select at least one course.';
	errors.value = found;
	return Object.keys(found).length === 0;
}

async function submit() {
	error.value = '';
	if (!validate()) return;

	busy.value = true;
	try {
		await auth.register({
			name: form.name.trim(),
			email: form.email.trim(),
			password: form.password,
			school: form.school,
			programme: form.programme.trim(),
			yearOfStudy: Number(form.yearOfStudy),
			contactNumber: form.contactNumber.trim(),
			telegramHandle: normalizeTelegramHandle(form.telegramHandle),
			courses: form.courses,
			newCourses: newCourses.value.map((course) => ({ courseCode: course.code, courseName: course.name })),
		});
		newCourses.value.forEach((course) => lookups.addCourse({ code: course.code, name: course.name, school: null }));
		const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') ? route.query.redirect : null;
		await router.push(redirect ?? auth.homePath);
	} catch (caught) {
		error.value = caught.message;
	} finally {
		busy.value = false;
	}
}
</script>

<template>
	<div class="card-flat p-4">
		<h2 class="h5 mb-1">Create your student account</h2>
		<p class="text-muted small mb-4">Add your profile details to get started.</p>

		<div v-if="error" class="alert alert-danger py-2" role="alert">{{ error }}</div>
		<div v-if="courseError" class="alert alert-danger py-2" role="alert">
			{{ courseError }}
			<button type="button" class="btn btn-link btn-sm p-0 ms-2" @click="loadCourses">Try again</button>
		</div>

		<form novalidate @submit.prevent="submit">
			<FormField label="Name" required :error="errors.name" v-slot="{ id, invalidClass }">
				<input :id="id" v-model="form.name" type="text" maxlength="100" class="form-control" :class="invalidClass" autocomplete="name" />
			</FormField>
			<FormField label="Email" required :error="errors.email" v-slot="{ id, invalidClass }">
				<input :id="id" v-model="form.email" type="email" maxlength="255" class="form-control" :class="invalidClass" autocomplete="email" />
			</FormField>
			<div class="row">
				<div class="col-md-6">
					<FormField label="Password" required hint="At least 6 characters." :error="errors.password" v-slot="{ id, invalidClass }">
						<input :id="id" v-model="form.password" type="password" class="form-control" :class="invalidClass" autocomplete="new-password" />
					</FormField>
				</div>
				<div class="col-md-6">
					<FormField label="Confirm password" required :error="errors.confirmPassword" v-slot="{ id, invalidClass }">
						<input :id="id" v-model="form.confirmPassword" type="password" class="form-control" :class="invalidClass" autocomplete="new-password" />
					</FormField>
				</div>
			</div>
			<FormField label="School" required :error="errors.school" v-slot="{ id, invalidClass }">
				<select :id="id" v-model="form.school" class="form-select" :class="invalidClass">
					<option value="" disabled>Select a school</option>
					<option v-for="school in SCHOOLS" :key="school" :value="school">{{ school }}</option>
				</select>
			</FormField>
			<FormField label="Programme" required :error="errors.programme" v-slot="{ id, invalidClass }">
				<input :id="id" v-model="form.programme" type="text" maxlength="100" class="form-control" :class="invalidClass" autocomplete="organization-title" />
			</FormField>
			<div class="row">
				<div class="col-md-6">
					<FormField label="Year of study" required :error="errors.yearOfStudy" v-slot="{ id, invalidClass }">
						<select :id="id" v-model="form.yearOfStudy" class="form-select" :class="invalidClass">
							<option value="" disabled>Select year</option>
							<option v-for="year in YEARS_OF_STUDY" :key="year" :value="year">{{ year }}</option>
						</select>
					</FormField>
				</div>
				<div class="col-md-6">
					<FormField label="Contact number" required :error="errors.contactNumber" v-slot="{ id, invalidClass }">
						<input :id="id" v-model="form.contactNumber" type="tel" class="form-control" :class="invalidClass" autocomplete="tel" placeholder="+65 9123 4567" />
					</FormField>
				</div>
			</div>
			<FormField label="Telegram username" hint="Optional." :error="errors.telegramHandle" v-slot="{ id, invalidClass }">
				<input :id="id" v-model="form.telegramHandle" type="text" class="form-control" :class="invalidClass" placeholder="@username" />
			</FormField>
			<FormField label="Courses you are taking" required :error="errors.courses" hint="Select at least one course.">
				<div v-if="loadingCourses" class="text-muted small">Loading courses…</div>
				<ChipSelect v-else v-model="form.courses" :options="courseOptions" :disabled="busy || courseOptions.length === 0" />
			</FormField>
			<div class="row g-2 align-items-end mb-3">
				<div class="col-sm-3">
					<label for="signup-course-code" class="form-label">New course code</label>
					<input id="signup-course-code" v-model="newCourseCode" class="form-control" maxlength="20" placeholder="IS442" :disabled="busy || loadingCourses" />
				</div>
				<div class="col-sm-6">
					<label for="signup-course-name" class="form-label">New course name</label>
					<input id="signup-course-name" v-model="newCourseName" class="form-control" maxlength="255" placeholder="Object-Oriented Application Development" :disabled="busy || loadingCourses" />
				</div>
				<div class="col-sm-3">
					<button type="button" class="btn btn-outline-primary w-100" :disabled="busy || loadingCourses" @click="addCourse">Add course</button>
				</div>
				<div v-if="newCourseError" class="small text-danger" role="alert">{{ newCourseError }}</div>
				<div v-else class="form-text">New courses join the shared catalogue after signup succeeds.</div>
			</div>
			<button type="submit" class="btn btn-primary w-100 mt-2" :disabled="busy || loadingCourses || courseOptions.length === 0">
				<span v-if="busy" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Create account
			</button>
		</form>

		<p class="text-center small mb-0 mt-3">
			Already have an account? <RouterLink :to="{ name: 'login' }">Sign in</RouterLink>
		</p>
	</div>
</template>
