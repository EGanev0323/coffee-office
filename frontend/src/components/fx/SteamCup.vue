<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { gsap, reducedMotion } from '../../motion'

/* Чаша кафе, „нарисувана с маркер“, от която се вдига пара. */
const props = defineProps({
  size: { type: Number, default: 96 },
  draw: { type: Boolean, default: true }, // чашата се дорисува при появата
  delay: { type: Number, default: 0 }
})

const root = ref(null)
let ctx = null

onMounted(() => {
  if (reducedMotion() || !root.value) return
  ctx = gsap.context(() => {
    const lines = root.value.querySelectorAll('.cup-line')
    const steam = root.value.querySelectorAll('.cup-steam')
    const intro = gsap.timeline({ delay: props.delay })

    if (props.draw) {
      lines.forEach((line) => {
        const len = line.getTotalLength()
        gsap.set(line, { strokeDasharray: len, strokeDashoffset: len })
      })
      intro
        .to(lines, { strokeDashoffset: 0, duration: 0.7, stagger: 0.12, ease: 'power2.inOut' })
        .from('.cup-coffee', { scaleY: 0, transformOrigin: '50% 50%', duration: 0.4, ease: 'back.out(2)' }, '-=0.2')
    }

    steam.forEach((path, i) => {
      gsap
        .timeline({ repeat: -1, delay: (props.draw ? intro.duration() : 0) + i * 0.55 })
        .fromTo(
          path,
          { y: 10, autoAlpha: 0, scaleX: 0.7 },
          { y: -4, autoAlpha: 0.85, scaleX: 1, duration: 1.1, ease: 'sine.out', svgOrigin: '60 40' }
        )
        .to(path, { y: -18, autoAlpha: 0, scaleX: 1.25, duration: 1, ease: 'sine.in', svgOrigin: '60 40' })
    })
  }, root.value)
})

onBeforeUnmount(() => ctx?.revert())
</script>

<template>
  <svg ref="root" class="steam-cup" :width="size" :height="size" viewBox="0 0 120 120" aria-hidden="true">
    <path class="cup-steam" d="M44 40c-6-7 5-11-1-19s3-11 0-15" />
    <path class="cup-steam" d="M60 40c-6-7 5-11-1-19s3-11 0-15" />
    <path class="cup-steam" d="M76 40c-6-7 5-11-1-19s3-11 0-15" />
    <path class="cup-line cup-body" d="M27 51l6.5 37c1.4 7.6 5.6 10 12.5 10h28c6.9 0 11.1-2.4 12.5-10L93 51" />
    <ellipse class="cup-line cup-rim" cx="60" cy="51" rx="33" ry="7" />
    <ellipse class="cup-coffee" cx="60" cy="51.5" rx="28" ry="4.6" />
    <path class="cup-line cup-handle" d="M90 60c15-3 18 20-3 22" />
    <path class="cup-line cup-saucer" d="M16 101c10 6 78 6 88 0" />
  </svg>
</template>

<style scoped>
.steam-cup { overflow: visible; display: block; }
.cup-line {
  fill: none;
  stroke: var(--ink);
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.cup-body { fill: #FFFFFF; }
.cup-coffee { fill: #6B4630; }
.cup-steam {
  fill: none;
  stroke: var(--muted);
  stroke-width: 3.2;
  stroke-linecap: round;
  opacity: 0.55;
}
</style>
