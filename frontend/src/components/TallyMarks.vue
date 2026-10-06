<script setup>
import { computed } from 'vue'

/*
  Балансът, нарисуван като чертички на дъска – групи по пет, петата е наклонена.
  Нова чертичка се „дописва“, а при изпито кафе последната се „изтрива“.
*/
const props = defineProps({
  count: { type: Number, required: true },
  max: { type: Number, default: 50 },
  size: { type: String, default: 'lg' }, // 'lg' | 'sm'
  unlimited: { type: Boolean, default: false }
})

const shown = computed(() => Math.max(0, Math.min(props.count, props.max)))
const hidden = computed(() => Math.max(0, props.count - shown.value))
const groups = computed(() => {
  const out = []
  for (let i = 0; i < shown.value; i += 5) out.push(Math.min(5, shown.value - i))
  return out
})

// Детерминистично „трептене“, за да изглеждат ръчно писани, без да мърдат при всяко прерисуване.
function jitter(a, b) {
  const v = Math.sin(a * 12.9898 + b * 78.233) * 43758.5453
  return v - Math.floor(v) - 0.5
}
function stroke(g, s) {
  const x = 7 + s * 8
  return {
    x1: x + jitter(g, s) * 2.2,
    y1: 5 + jitter(g, s + 11) * 2.5,
    x2: x + jitter(g, s + 3) * 2.8,
    y2: 37 + jitter(g, s + 7) * 2.5
  }
}
function slash(g) {
  return { x1: 2, y1: 31 + jitter(g, 21) * 3, x2: 38, y2: 10 + jitter(g, 23) * 3 }
}
</script>

<template>
  <div v-if="unlimited" class="tally" :class="`tally-${size}`" role="img" aria-label="безкрайни кафета">
    <svg class="tally-infinity" viewBox="0 0 64 32" aria-hidden="true">
      <path
        class="tally-stroke tally-infinity-path"
        d="M32 16 C26 5, 6 4, 5 16 C4 28, 25 28, 32 16 C39 4, 59 4, 59 16 C59 28, 38 27, 32 16"
      />
    </svg>
    <span class="tally-more" aria-hidden="true">+1</span>
  </div>
  <div
    v-else
    class="tally"
    :class="`tally-${size}`"
    role="img"
    :aria-label="`${count} ${count === 1 ? 'кафе' : 'кафета'}`"
  >
    <TransitionGroup name="tally-group">
      <svg v-for="(n, g) in groups" :key="g" class="tally-group" viewBox="0 0 40 42" aria-hidden="true">
        <TransitionGroup name="tally-erase" tag="g">
          <line
            v-for="s in Math.min(n, 4)"
            :key="s"
            v-bind="stroke(g, s - 1)"
            :data-stroke="g * 5 + s - 1"
            class="tally-stroke"
          />
          <line v-if="n === 5" key="slash" v-bind="slash(g)" :data-stroke="g * 5 + 4" class="tally-stroke" />
        </TransitionGroup>
      </svg>
    </TransitionGroup>
    <span v-if="hidden > 0" class="tally-more">и още {{ hidden }}</span>
  </div>
</template>

<style scoped>
.tally {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem 0.9rem;
  min-height: 42px;
}
.tally-group { width: 40px; height: 42px; overflow: visible; }
.tally-sm { gap: 0.25rem 0.6rem; min-height: 32px; }
.tally-sm .tally-group { width: 30px; height: 32px; }

.tally-stroke {
  stroke: var(--marker);
  stroke-width: 3.2;
  stroke-linecap: round;
  stroke-dasharray: 44;
  animation: draw 0.4s ease-out both;
}
.tally-infinity { width: 88px; height: 44px; overflow: visible; }
.tally-sm .tally-infinity { width: 60px; height: 30px; }
.tally-infinity-path {
  fill: none;
  stroke-dasharray: 150;
  animation-name: draw-long;
  animation-duration: 0.7s;
}
.tally-more {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 1.5rem;
  color: var(--marker);
  line-height: 1;
}

/* изтриване на една чертичка */
.tally-stroke.tally-erase-leave-active,
.tally-group-leave-active .tally-stroke {
  animation: erase 0.55s ease-in forwards;
}
/* цялата група изчезва, когато в нея не остане чертичка */
.tally-group.tally-group-leave-active {
  animation: fade-out 0.55s ease-in forwards;
}

@keyframes draw {
  from { stroke-dashoffset: 44; }
  to { stroke-dashoffset: 0; }
}
@keyframes draw-long {
  from { stroke-dashoffset: 150; }
  to { stroke-dashoffset: 0; }
}
@keyframes erase {
  0% { stroke-dashoffset: 0; opacity: 1; }
  100% { stroke-dashoffset: -44; opacity: 0; }
}
@keyframes fade-out {
  to { opacity: 0; }
}
</style>
