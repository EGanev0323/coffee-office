<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { finePointer, gsap, reducedMotion } from '../../motion'
import { BEAN } from '../../motion/burst'

/*
  Кафе зърна, които се носят бавно във фона и леко бягат от курсора (паралакс).
  Само декорация – без събития с мишката и скрити от четците на екрана.
*/
const BEANS = [
  { x: 6, y: 14, s: 34, r: -20, d: 0.6 },
  { x: 88, y: 9, s: 26, r: 35, d: 0.4 },
  { x: 92, y: 62, s: 40, r: -45, d: 0.9 },
  { x: 3, y: 74, s: 28, r: 60, d: 0.5 },
  { x: 48, y: 92, s: 22, r: 10, d: 0.3 },
  { x: 70, y: 30, s: 18, r: -70, d: 0.25 },
  { x: 24, y: 44, s: 16, r: 80, d: 0.2 }
]

const root = ref(null)
let ctx = null
let onMove = null

onMounted(() => {
  if (reducedMotion() || !root.value) return
  ctx = gsap.context(() => {
    const beans = gsap.utils.toArray('.float-bean')
    gsap.from(beans, { scale: 0, autoAlpha: 0, duration: 1, stagger: 0.08, ease: 'back.out(2)', delay: 0.2 })

    beans.forEach((bean) => {
      gsap.to(bean.firstElementChild, {
        y: gsap.utils.random(-18, 18),
        x: gsap.utils.random(-10, 10),
        rotation: `+=${gsap.utils.random(-25, 25)}`,
        duration: gsap.utils.random(3, 5.5),
        ease: 'sine.inOut',
        yoyo: true,
        repeat: -1
      })
    })

    if (finePointer()) {
      const movers = beans.map((bean, i) => ({
        depth: BEANS[i].d,
        x: gsap.quickTo(bean, 'x', { duration: 1.2, ease: 'power3.out' }),
        y: gsap.quickTo(bean, 'y', { duration: 1.2, ease: 'power3.out' })
      }))
      onMove = (e) => {
        const nx = e.clientX / window.innerWidth - 0.5
        const ny = e.clientY / window.innerHeight - 0.5
        movers.forEach((m) => {
          m.x(-nx * 60 * m.depth)
          m.y(-ny * 60 * m.depth)
        })
      }
      window.addEventListener('pointermove', onMove, { passive: true })
    }
  }, root.value)
})

onBeforeUnmount(() => {
  if (onMove) window.removeEventListener('pointermove', onMove)
  ctx?.revert()
})
</script>

<template>
  <div ref="root" class="floating-beans" aria-hidden="true">
    <span
      v-for="(b, i) in BEANS"
      :key="i"
      class="float-bean"
      :style="{ left: `${b.x}%`, top: `${b.y}%`, width: `${b.s}px`, height: `${b.s * 1.3}px`, opacity: 0.12 + b.d * 0.22 }"
    >
      <span class="float-bean-inner" :style="{ transform: `rotate(${b.r}deg)` }" v-html="BEAN"></span>
    </span>
  </div>
</template>

<style scoped>
.floating-beans {
  position: fixed;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}
.float-bean { position: absolute; display: block; }
.float-bean-inner { display: block; width: 100%; height: 100%; }
</style>
