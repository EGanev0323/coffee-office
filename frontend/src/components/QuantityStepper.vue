<script setup>
/* Избор на брой с бутони − и +. Използва се с v-model. */
const props = defineProps({
  modelValue: { type: Number, required: true },
  min: { type: Number, default: 1 },
  max: { type: Number, default: 100 }, // като BuyRequest.MAX_QUANTITY
  disabled: { type: Boolean, default: false },
  label: { type: String, default: 'Брой' }
})
const emit = defineEmits(['update:modelValue'])

function change(delta) {
  const next = Math.min(props.max, Math.max(props.min, props.modelValue + delta))
  if (next !== props.modelValue) emit('update:modelValue', next)
}
</script>

<template>
  <div class="stepper" role="group" :aria-label="label">
    <button
      type="button"
      class="stepper-btn"
      :disabled="disabled || modelValue <= min"
      :aria-label="`${label}: с един по-малко`"
      @click="change(-1)"
    >−</button>
    <output class="stepper-value" aria-live="polite">{{ modelValue }}</output>
    <button
      type="button"
      class="stepper-btn"
      :disabled="disabled || modelValue >= max"
      :aria-label="`${label}: с един повече`"
      @click="change(1)"
    >+</button>
  </div>
</template>

<style scoped>
.stepper {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--rule);
  border-radius: 8px;
  background: var(--surface);
}
.stepper-btn {
  font: inherit;
  font-size: 1.25rem;
  font-weight: 600;
  line-height: 1;
  width: 2.5rem;
  height: 2.5rem;
  border: 0;
  background: none;
  color: var(--ink);
  cursor: pointer;
  border-radius: 8px;
}
.stepper-btn:hover:not(:disabled) { background: rgba(43, 31, 24, 0.05); }
.stepper-btn:disabled { color: var(--rule); cursor: not-allowed; }
.stepper-value {
  min-width: 2ch;
  text-align: center;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
</style>
