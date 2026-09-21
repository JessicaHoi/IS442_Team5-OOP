<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { studentsApi } from '../../api/students.api';
import { useLookupsStore } from '../../stores/lookups';
import { useAuthStore } from '../../stores/auth';
import { useToast } from '../../composables/useToast';
import { useAsync } from '../../composables/useAsync';
import { GROUP_FORMATS, MEETING_MODES, SCHOOLS, STUDY_GOALS, YEARS_OF_STUDY } from '../../utils/constants';
import { validateSlots } from '../../utils/availability';
import PageHeader from '../../components/ui/PageHeader.vue';
import FormField from '../../components/ui/FormField.vue';
import ChipSelect from '../../components/ui/ChipSelect.vue';
import SegmentedControl from '../../components/ui/SegmentedControl.vue';
import AvailabilityEditor from '../../components/ui/AvailabilityEditor.vue';
import LoadingState from '../../components/ui/LoadingState.vue';
import ErrorState from '../../components/ui/ErrorState.vue';

const PHONE_PATTERN = /^\+?[\d\s-]{8,15}$/;

const lookups = useLookupsStore();
const auth = useAuthStore();
const toast = useToast();

const profile = reactive({ name: '', school: '', programme: '', yearOfStudy: '', contactNumber: '', courses: [] });
const prefs = reactive({ course: '', meetingMode: 'EITHER', groupFormat: 'EITHER', goals: [], availability: [] });
const errors = ref({});
const submitted = ref(false);
const saving = ref(false);

const courseOptions = computed(() =>
	lookups.courses.map((course) => ({ value: course.code, label: course.code, title: course.name })),
);
const takenCourses = computed(() => lookups.courses.filter((course) => profile.courses.includes(course.code)));

function apply(me) {
	Object.assign(profile, {
		name: me.name,
		school: me.school,
		programme: me.programme,
		yearOfStudy: me.yearOfStudy ?? '',
		contactNumber: me.contactNumber ?? '',
		courses: [...me.courses],
	});
	Object.assign(prefs, {
		course: me.preferences.course ?? '',
		meetingMode: me.preferences.meetingMode,
		groupFormat: me.preferences.groupFormat,
		goals: [...me.preferences.goals],
		availability: me.preferences.availability.map((slot) => ({ ...slot })),
	});
}

const { loading, error, run: load } = useAsync(async () => {
	await lookups.loadCourses();
	apply(await studentsApi.getMe());
});

onMounted(load);

// A buddy course must be one of the courses the student is taking.
watch(
	() => profile.courses,
	(codes) => {
		if (prefs.course && !codes.includes(prefs.course)) prefs.course = '';
	},
);

function validate() {
	const found = {};
	if (!profile.name.trim()) found.name = 'Enter your name.';
	if (!profile.school) found.school = 'Choose your school.';
	if (!profile.programme.trim()) found.programme = 'Enter your programme.';
	if (!profile.yearOfStudy) found.yearOfStudy = 'Choose your year.';
	if (!PHONE_PATTERN.test(profile.contactNumber.trim())) found.contactNumber = 'Enter a valid phone number, e.g. +65 9123 4567.';
	if (profile.courses.length === 0) found.courses = 'Select at least one course.';
	if (!prefs.course) found.course = 'Choose the course you need a study buddy for.';
	if (prefs.goals.length === 0) found.goals = 'Choose at least one study goal.';
	if (prefs.availability.length === 0 || validateSlots(prefs.availability).some(Boolean)) {
		found.availability = 'Add at least one valid time slot.';
	}
	errors.value = found;
	return Object.keys(found).length === 0;
}

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
		const saved = await studentsApi.updatePreferences({ ...prefs, availability: prefs.availability });
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
				<h2 class="h6 mb-3">Study preferences</h2>

				<div class="row">
					<div class="col-md-8">
						<FormField
							label="Course I need a study buddy for"
							required
							:error="errors.course"
							:hint="takenCourses.length === 0 ? 'Select your courses above first.' : ''"
							v-slot="{ id, invalidClass }"
						>
							<select :id="id" v-model="prefs.course" class="form-select" :class="invalidClass" :disabled="takenCourses.length === 0">
								<option value="" disabled>Select a course</option>
								<option v-for="course in takenCourses" :key="course.code" :value="course.code">
									{{ course.code }} · {{ course.name }}
								</option>
							</select>
						</FormField>
					</div>
				</div>

				<div class="row">
					<div class="col-md-6 mb-3">
						<div class="form-label">Meeting mode</div>
						<SegmentedControl v-model="prefs.meetingMode" :options="MEETING_MODES" />
					</div>
					<div class="col-md-6 mb-3">
						<div class="form-label">Group format</div>
						<SegmentedControl v-model="prefs.groupFormat" :options="GROUP_FORMATS" />
					</div>
				</div>

				<div class="mb-3">
					<div class="form-label">Study goals <span class="text-danger">*</span></div>
					<ChipSelect v-model="prefs.goals" :options="STUDY_GOALS" />
					<div v-if="errors.goals" class="invalid-feedback d-block">{{ errors.goals }}</div>
				</div>

				<div>
					<div class="form-label">Weekly availability <span class="text-danger">*</span></div>
					<AvailabilityEditor v-model="prefs.availability" />
					<div v-if="errors.availability" class="invalid-feedback d-block">{{ errors.availability }}</div>
				</div>
			</section>

			<div class="save-bar d-flex justify-content-end align-items-center gap-3 py-3">
				<span v-if="submitted && Object.keys(errors).length" class="small text-danger">
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
