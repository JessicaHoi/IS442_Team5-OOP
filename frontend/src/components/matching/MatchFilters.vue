<script setup>
import { computed, ref } from 'vue';
import { DAYS, GROUP_FORMATS, MEETING_MODES, STUDY_GOALS } from '../../utils/constants';
import { useLookupsStore } from '../../stores/lookups';

const props = defineProps({
	/** { course, goal, day, meetingMode, groupFormat } (empty string = no filter) */
	modelValue: { type: Object, required: true },
});
const emit = defineEmits(['update:modelValue', 'reset']);

const lookups = useLookupsStore();

// On small screens the extra filters are tucked behind a toggle to keep results in view.
const showFilters = ref(false);
const activeFilterCount = computed(() => ['day', 'meetingMode', 'groupFormat'].filter((key) => props.modelValue[key]).length);

function update(key, value) {
	emit('update:modelValue', { ...props.modelValue, [key]: value });
}
</script>

<template>
	<div class="card-flat p-3 p-md-4">
		<div class="section-title">Search by</div>
		<div class="row g-3 mb-4">
			<div class="col-md-6">
				<label for="filter-course" class="form-label">Course</label>
				<select id="filter-course" class="form-select" :value="modelValue.course" @change="update('course', $event.target.value)">
					<option value="">Any course</option>
					<option v-for="course in lookups.courses" :key="course.code" :value="course.code">
						{{ course.code }} · {{ course.name }}
					</option>
				</select>
			</div>
			<div class="col-md-6">
				<label for="filter-goal" class="form-label">Study goal</label>
				<select id="filter-goal" class="form-select" :value="modelValue.goal" @change="update('goal', $event.target.value)">
					<option value="">Any goal</option>
					<option v-for="goal in STUDY_GOALS" :key="goal.value" :value="goal.value">{{ goal.label }}</option>
				</select>
			</div>
		</div>

		<button
			type="button"
			class="btn btn-light border w-100 d-md-none"
			:aria-expanded="showFilters"
			aria-controls="extra-filters"
			@click="showFilters = !showFilters"
		>
			<i class="bi bi-funnel me-1"></i>Filters
			<span v-if="activeFilterCount" class="badge text-bg-primary rounded-pill ms-1">{{ activeFilterCount }}</span>
			<i class="bi ms-1" :class="showFilters ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
		</button>

		<div id="extra-filters" :class="showFilters ? 'mt-3' : 'd-none d-md-block'">
			<div class="section-title d-none d-md-block">Filter by</div>
			<div class="row g-3 align-items-end">
				<div class="col-sm-6 col-lg-3">
					<label for="filter-day" class="form-label">Available on</label>
					<select id="filter-day" class="form-select" :value="modelValue.day" @change="update('day', $event.target.value)">
						<option value="">Any day</option>
						<option v-for="day in DAYS" :key="day.value" :value="day.value">{{ day.long }}</option>
					</select>
				</div>
				<div class="col-sm-6 col-lg-3">
					<label for="filter-mode" class="form-label">Meeting mode</label>
					<select id="filter-mode" class="form-select" :value="modelValue.meetingMode" @change="update('meetingMode', $event.target.value)">
						<option value="">Any mode</option>
						<option v-for="mode in MEETING_MODES" :key="mode.value" :value="mode.value">{{ mode.label }}</option>
					</select>
				</div>
				<div class="col-sm-6 col-lg-3">
					<label for="filter-format" class="form-label">Group format</label>
					<select id="filter-format" class="form-select" :value="modelValue.groupFormat" @change="update('groupFormat', $event.target.value)">
						<option value="">Any format</option>
						<option v-for="format in GROUP_FORMATS" :key="format.value" :value="format.value">{{ format.label }}</option>
					</select>
				</div>
				<div class="col-sm-6 col-lg-3">
					<button type="button" class="btn btn-light border w-100" @click="$emit('reset')">
						<i class="bi bi-arrow-counterclockwise me-1"></i>Reset filters
					</button>
				</div>
			</div>
		</div>
	</div>
</template>
