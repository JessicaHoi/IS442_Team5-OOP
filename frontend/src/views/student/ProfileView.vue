<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { studentsApi } from '../../api/students.api';
import { useLookupsStore } from '../../stores/lookups';
import { useAuthStore } from '../../stores/auth';
import { useToast } from '../../composables/useToast';
import { useAsync } from '../../composables/useAsync';
import { SCHOOLS, YEARS_OF_STUDY } from '../../utils/constants';
import { validateSlots } from '../../utils/availability';
import { emptyPreference } from '../../utils/preferences';
import PageHeader from '../../components/ui/PageHeader.vue';
import FormField from '../../components/ui/FormField.vue';
import ChipSelect from '../../components/ui/ChipSelect.vue';
import PreferenceEditor from '../../components/profile/PreferenceEditor.vue';
import LoadingState from '../../components/ui/LoadingState.vue';
import ErrorState from '../../components/ui/ErrorState.vue';

const PHONE_PATTERN = /^\+?[\d\s-]{8,15}$/;

const lookups = useLookupsStore();
const auth = useAuthStore();
const toast = useToast();

const profile = reactive({ name: '', school: '', programme: '', yearOfStudy: '', contactNumber: '', courses: [] });
/** One study preference per course the student needs a buddy for. */
const prefs = ref([]);
const courseToAdd = ref('');
const errors = ref({});
/** Per-preference errors, keyed by course code. */
const preferenceErrors = ref({});
const submitted = ref(false);
const saving = ref(false);

const courseOptions = computed(() =>
	lookups.courses.map((course) => ({ value: course.code, label: course.code, title: course.name })),
);
const takenCourses = computed(() => lookups.courses.filter((course) => profile.courses.includes(course.code)));
/** Courses the student takes but has no preference for yet. */
const addableCourses = computed(() =>
	takenCourses.value.filter((course) => !prefs.value.some((preference) => preference.course === course.code)),
);

function apply(me) {
	Object.assign(profile, {
		name: me.name,
		school: me.school,
		programme: me.programme,
		yearOfStudy: me.yearOfStudy ?? '',
		contactNumber: me.contactNumber ?? '',
		courses: [...me.courses],
	});
	prefs.value = me.preferences.map((preference) => ({
		...preference,
		goals: [...preference.goals],
		availability: preference.availability.map((slot) => ({ ...slot })),
	}));
}

const { loading, error, run: load } = useAsync(async () => {
	await lookups.loadCourses();
	apply(await studentsApi.getMe());
});

onMounted(load);

// A buddy course must be one of the courses the student is taking (the server enforces the same rule).
watch(
	() => profile.courses,
	(codes) => {
		prefs.value = prefs.value.filter((preference) => codes.includes(preference.course));
		if (courseToAdd.value && !codes.includes(courseToAdd.value)) courseToAdd.value = '';
	},
);

function addPreference() {
	if (!courseToAdd.value) return;
	prefs.value = [...prefs.value, emptyPreference(courseToAdd.value)];
	courseToAdd.value = '';
}

function updatePreference(index, preference) {
	prefs.value = prefs.value.map((current, i) => (i === index ? preference : current));
}

function removePreference(index) {
	prefs.value = prefs.value.filter((_, i) => i !== index);
}

function validate() {
	const found = {};
	if (!profile.name.trim()) found.name = 'Enter your name.';
	if (!profile.school) found.school = 'Choose your school.';
	if (!profile.programme.trim()) found.programme = 'Enter your programme.';
	if (!profile.yearOfStudy) found.yearOfStudy = 'Choose your year.';
	if (!PHONE_PATTERN.test(profile.contactNumber.trim())) found.contactNumber = 'Enter a valid phone number, e.g. +65 9123 4567.';
	if (profile.courses.length === 0) found.courses = 'Select at least one course.';
	if (prefs.value.length === 0) found.preferences = 'Add a preference for at least one course you need a study buddy for.';

	const foundPerCourse = {};
	for (const preference of prefs.value) {
		const problems = {};
		if (preference.goals.length === 0) problems.goals = 'Choose at least one study goal.';
		if (preference.availability.length === 0 || validateSlots(preference.availability).some(Boolean)) {
			problems.availability = 'Add at least one valid time slot.';
		}
		if (Object.keys(problems).length) foundPerCourse[preference.course] = problems;
	}

	errors.value = found;
	preferenceErrors.value = foundPerCourse;
	return Object.keys(found).length === 0 && Object.keys(foundPerCourse).length === 0;
}

const hasErrors = computed(() => Object.keys(errors.value).length > 0 || Object.keys(preferenceErrors.value).length > 0);

// Once a save has been attempted, re-check as the student fixes each field.
watch([profile, prefs], () => submitted.value && validate(), { deep: true });

async function save() {
	submitted.value = true;
	if (!validate()) {
		toast.error('Please fix the highlighted fields.');
		return;
	}
	saving.value = true;
	try {
		await studentsApi.updateProfile({
			name: profile.name.trim(),
			school: profile.school,
			programme: profile.programme.trim(),
			yearOfStudy: Number(profile.yearOfStudy),
			contactNumber: profile.contactNumber.trim(),
			courses: profile.courses,
		});
		const saved = await studentsApi.updatePreferences(prefs.value);
		auth.setDisplayName(profile.name.trim());
		apply(saved);
		submitted.value = false;
		toast.success('Profile saved.');
	} catch (caught) {
		toast.error(caught.message);
	} finally {
		saving.value = false;
	}
}
</script>

<template>
	<div>
		<PageHeader title="My profile" subtitle="Your details and study preferences are used to find your best matches." />

		<LoadingState v-if="loading" />
		<ErrorState v-else-if="error" :message="error" @retry="load" />

		<form v-else novalidate @submit.prevent="save">
			<section class="card-flat p-4 mb-4">
				<h2 class="h6 mb-3">About you</h2>
				<div class="row">
					<div class="col-md-6">
						<FormField label="Full name" required :error="errors.name" v-slot="{ id, invalidClass }">
							<input :id="id" v-model="profile.name" class="form-control" :class="invalidClass" autocomplete="name" />
						</FormField>
					</div>
					<div class="col-md-6">
						<FormField label="Contact number" required :error="errors.contactNumber" hint="Only shown to students you are connected with." v-slot="{ id, invalidClass }">
							<input :id="id" v-model="profile.contactNumber" type="tel" class="form-control" :class="invalidClass" autocomplete="tel" />
						</FormField>
					</div>
					<div class="col-md-6">
						<FormField label="School" required :error="errors.school" v-slot="{ id, invalidClass }">
							<select :id="id" v-model="profile.school" class="form-select" :class="invalidClass">
								<option value="" disabled>Select your school</option>
								<option v-for="school in SCHOOLS" :key="school" :value="school">{{ school }}</option>
							</select>
						</FormField>
					</div>
					<div class="col-md-4">
						<FormField label="Programme" required :error="errors.programme" v-slot="{ id, invalidClass }">
							<input :id="id" v-model="profile.programme" class="form-control" :class="invalidClass" />
						</FormField>
					</div>
					<div class="col-md-2">
						<FormField label="Year" required :error="errors.yearOfStudy" v-slot="{ id, invalidClass }">
							<select :id="id" v-model="profile.yearOfStudy" class="form-select" :class="invalidClass">
								<option value="" disabled>Year</option>
								<option v-for="year in YEARS_OF_STUDY" :key="year" :value="year">{{ year }}</option>
							</select>
						</FormField>
					</div>
				</div>

				<div>
					<div class="form-label">Courses you are taking <span class="text-danger">*</span></div>
					<ChipSelect v-model="profile.courses" :options="courseOptions" />
					<div v-if="errors.courses" class="invalid-feedback d-block">{{ errors.courses }}</div>
				</div>
			</section>

			<section class="card-flat p-4 mb-4">
				<h2 class="h6 mb-1">Study preferences</h2>
				<p class="text-muted small mb-3">Add one preference for each course you need a study buddy for. Matches for a course use that course's preference.</p>

				<div class="d-flex flex-column gap-3 mb-3">
					<PreferenceEditor
						v-for="(preference, index) in prefs"
						:key="preference.course"
						:model-value="preference"
						:course-label="lookups.courseLabel(preference.course)"
						:errors="preferenceErrors[preference.course]"
						@update:model-value="updatePreference(index, $event)"
						@remove="removePreference(index)"
					/>
				</div>

				<div class="row g-2 align-items-end">
					<div class="col-md-8">
						<label for="add-preference-course" class="form-label">Add a course I need a study buddy for</label>
						<select id="add-preference-course" v-model="courseToAdd" class="form-select" :disabled="addableCourses.length === 0">
							<option value="" disabled>
								{{ takenCourses.length === 0 ? 'Select your courses above first' : addableCourses.length === 0 ? 'Every course has a preference' : 'Select a course' }}
							</option>
							<option v-for="course in addableCourses" :key="course.code" :value="course.code">
								{{ course.code }} · {{ course.name }}
							</option>
						</select>
					</div>
					<div class="col-md-4">
						<button type="button" class="btn btn-outline-primary w-100" :disabled="!courseToAdd" @click="addPreference">
							<i class="bi bi-plus-lg me-1"></i>Add preference
						</button>
					</div>
				</div>
				<div v-if="errors.preferences" class="invalid-feedback d-block">{{ errors.preferences }}</div>
			</section>

			<div class="save-bar d-flex justify-content-end align-items-center gap-3 py-3">
				<span v-if="submitted && hasErrors" class="small text-danger">
					<i class="bi bi-exclamation-circle me-1"></i>Some fields need attention.
				</span>
				<button type="submit" class="btn btn-primary px-4" :disabled="saving">
					<span v-if="saving" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Save profile
				</button>
			</div>
		</form>
	</div>
</template>

<style scoped>
.save-bar {
	position: sticky;
	bottom: 0;
	background: linear-gradient(to top, var(--bs-body-bg) 70%, transparent);
}
</style>
