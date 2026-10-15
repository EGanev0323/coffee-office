<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { gsap, pop, reducedMotion } from '../../motion'

/*
  Число, което се „навърта“ до стойността си (по CountUp от React Bits).
  При промяна тръгва от старата стойност към новата и подскача.
*/
const props = defineProps({
  value: { type: Number, required: true },
  from: { type: Number, default: 0 },
  duration: { type: Number, default: 1.2 },
  delay: { type: Number, default: 0 },
  format: { type: Function, default: (v) => String(Math.round(v)) },
  bounce: { type: Boolean, default: true }
})

const el = ref(null)
const current = ref(reducedMotion() ? props.value : props.from)
const state = { v: current.value }
let tween = null

const text = computed(() => props.format(current.value))

function animateTo(target, { delay = 0, bounce = false } = {}) {
  tween?.kill()
  if (reducedMotion()) {
    state.v = target
    current.value = target
    return
  }
  tween = gsap.to(state, {
    v: target,
    duration: props.duration,
    delay,
    ease: 'power3.out',
    onUpdate: () => { current.value = state.v },
    onComplete: () => { current.value = target }
  })
  if (bounce && props.bounce) pop(el.value)
}

onMounted(() => animateTo(props.value, { delay: props.delay }))
watch(() => props.value, (v, old) => animateTo(v, { bounce: v !== old }))
onBeforeUnmount(() => tween?.kill())
</script>

<template>
  <span ref="el" class="count-up">{{ text }}</span>
</template>

<style scoped>
.count-up { display: inline-block; font-variant-numeric: tabular-nums; transform-origin: 50% 70%; }
</style>
