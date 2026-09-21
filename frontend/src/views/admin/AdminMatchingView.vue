<script setup>
import { computed, onMounted, ref } from 'vue';
import { adminApi } from '../../api/admin.api';
import { useToast } from '../../composables/useToast';
import { useAsync } from '../../composables/useAsync';
import { DEFAULT_MATCHING_CONFIG, MATCHING_CRITERIA, MATCHING_STRATEGIES } from '../../utils/constants';
import PageHeader from '../../components/ui/PageHeader.vue';
import LoadingState from '../../components/ui/LoadingState.vue';
import ErrorState from '../../components/ui/ErrorState.vue';
import WeightRow from '../../components/admin/WeightRow.vue';

const toast = useToast();

const config = ref(null);
const savedSnapshot = ref('');
const saving = ref(false);

const { loading, error, run: load } = useAsync(async () => {
	setConfig(await adminApi.getMatchingConfig());
});
onMounted(load);

function setConfig(value) {
	config.value = structuredClone(value);
	savedSnapshot.value = JSON.stringify(value);
}

const isDirty = computed(() => config.value !== null && JSON.stringify(config.value) !== savedSnapshot.value);
const activeWeightTotal = computed(() =>
	config.value.criteria.filter((c) => c.enabled).reduce((sum, c) => sum + c.weight, 0),
);
const isValid = computed(() => activeWeightTotal.value > 0);

const criterionFor = (key) => config.value.criteria.find((c) => c.key === key);

function shareOf(criterion) {
	if (!criterion.enabled || activeWeightTotal.value === 0) return 0;
	return Math.round((criterion.weight / activeWeightTotal.value) * 100);
}

function updateCriterion(updated) {
	config.value.criteria = config.value.criteria.map((c) => (c.key === updated.key ? updated : c));
}

function resetToDefaults() {
	config.value = structuredClone(DEFAULT_MATCHING_CONFIG);
}

async function save() {
	saving.value = true;
	try {
		setConfig(await adminApi.updateMatchingConfig(config.value));
		toast.success('Matching configuration saved. It applies to the next search.');
	} catch (caught) {
		toast.error(caught.message);
	} finally {
		saving.value = false;
	}
}
</script>

<template>
	<div>
		<PageHeader title="Matching configuration" subtitle="Choose how students are ranked and how much each criterion matters." />

		<LoadingState v-if="loading && !config" />
		<ErrorState v-else-if="error" :message="error" @retry="load" />

		<template v-else-if="config">
			<section class="mb-4">
				<div class="section-title">Matching strategy</div>
				<div class="row g-3" role="radiogroup" aria-label="Matching strategy">
					<div v-for="strategy in MATCHING_STRATEGIES" :key="strategy.value" class="col-md-4">
						<label class="strategy-card card-flat h-100 p-3 d-block" :class="{ selected: config.strategy === strategy.value }">
							<input v-model="config.strategy" type="radio" class="visually-hidden" name="strategy" :value="strategy.value" />
							<div class="d-flex align-items-center gap-2 mb-1">
								<i class="bi" :class="strategy.icon"></i>
								<span class="fw-semibold">{{ strategy.label }}</span>
								<i v-if="config.strategy === strategy.value" class="bi bi-check-circle-fill text-primary ms-auto"></i>
							</div>
							<p class="small text-muted mb-0">{{ strategy.description }}</p>
						</label>
					</div>
				</div>
			</section>

			<section class="card-flat p-3 p-md-4 mb-4">
				<div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-1">
					<div class="section-title mb-0">Criteria and weights</div>
					<span class="small text-muted">Shares are calculated from the enabled criteria.</span>
				</div>
				<WeightRow
					v-for="meta in MATCHING_CRITERIA"
					:key="meta.key"
					:criterion="criterionFor(meta.key)"
					:meta="meta"
					:share="shareOf(criterionFor(meta.key))"
					@update:criterion="updateCriterion"
				/>
				<div v-if="!isValid" class="alert alert-warning py-2 mt-3 mb-0" role="alert">
					<i class="bi bi-exclamation-triangle me-2"></i>Enable at least one criterion with a weight above 0.
				</div>
			</section>

			<div class="d-flex flex-wrap justify-content-end gap-2">
				<button type="button" class="btn btn-light border" :disabled="saving" @click="resetToDefaults">Reset to defaults</button>
				<button type="button" class="btn btn-primary px-4" :disabled="!isDirty || !isValid || saving" @click="save">
					<span v-if="saving" class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Save changes
				</button>
			</div>
		</template>
	</div>
</template>

<style scoped>
.strategy-card {
	cursor: pointer;
	transition: border-color 0.12s, background-color 0.12s;
}
.strategy-card:hover {
	border-color: var(--bs-primary);
}
.strategy-card.selected {
	border-color: var(--bs-primary);
	background: rgba(79, 70, 229, 0.04);
}
.strategy-card:has(input:focus-visible) {
	outline: 2px solid rgba(79, 70, 229, 0.4);
	outline-offset: 2px;
}
</style>
