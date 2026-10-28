<script setup>
import { MEETING_MODES, STUDY_GOALS, labelOf } from '../../utils/constants';
import AvailabilitySummary from '../ui/AvailabilitySummary.vue';

defineProps({ group: { type: Object, required: true } });
</script>

<template>
	<section aria-label="Group details">
		<h2 class="h5 mb-3">About this group</h2>
		<p class="text-break" style="white-space: pre-line">{{ group.description }}</p>
		<dl class="row mb-3">
			<dt class="col-sm-4 mb-2">Course</dt>
			<dd class="col-sm-8 mb-2">{{ group.courseCode }} · {{ group.courseName }}</dd>
			<dt class="col-sm-4 mb-2">Group leader</dt>
			<dd class="col-sm-8 mb-2">{{ group.leader.name }}</dd>
			<dt class="col-sm-4 mb-2">Members</dt>
			<dd class="col-sm-8 mb-2">{{ group.memberCount }} / {{ group.maxSize }}</dd>
			<dt class="col-sm-4 mb-2">Meeting mode</dt>
			<dd class="col-sm-8 mb-2">{{ labelOf(MEETING_MODES, group.meetingMode) }}</dd>
			<dt class="col-sm-4 mb-2">Study goals</dt>
			<dd class="col-sm-8 mb-2 d-flex flex-wrap gap-1">
				<span v-for="goal in group.goals" :key="goal" class="chip-static">{{ labelOf(STUDY_GOALS, goal) }}</span>
			</dd>
		</dl>
		<h3 class="h6">Weekly availability</h3>
		<AvailabilitySummary :slots="group.availability" />
		<slot />
	</section>
</template>
