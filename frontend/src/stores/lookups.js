import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { coursesApi } from '../api/courses.api';

/** Reference data that rarely changes: the course catalogue. */
export const useLookupsStore = defineStore('lookups', () => {
	const courses = ref([]);
	const loaded = ref(false);

	const byCode = computed(() => Object.fromEntries(courses.value.map((course) => [course.code, course])));

	async function loadCourses() {
		if (loaded.value) return;
		courses.value = await coursesApi.list();
		loaded.value = true;
	}

	/** "IS442 · Object-Oriented Application Development" */
	function courseLabel(code) {
		const course = byCode.value[code];
		return course ? `${course.code} · ${course.name}` : code;
	}

	return { courses, loaded, byCode, loadCourses, courseLabel };
});
