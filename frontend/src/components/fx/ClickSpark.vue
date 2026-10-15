<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { MARKER_COLORS, reducedMotion } from '../../motion'

/*
  Искри при всяко натискане на бутон или линк (по ClickSpark от React Bits) – малки черти в цветовете на маркерите.
  Едно платно за целия екран, което рисува само докато има живи искри.
  Не пречи при рисуване по дъската и при писане в полета.
*/
const props = defineProps({
  count: { type: Number, default: 9 },
  radius: { type: Number, default: 26 },
  size: { type: Number, default: 11 },
  width: { type: Number, default: 2.4 },
  duration: { type: Number, default: 480 }
})

const TARGETS = 'button, a, [role="button"], .btn, label.check'
const SKIP = 'canvas, input, textarea, select, .marker, .eraser'

const canvas = ref(null)
let ctx = null
let raf = 0
let ratio = 1
const bursts = []

const easeOut = (t) => 1 - Math.pow(1 - t, 3)

function resize() {
  const c = canvas.value
  if (!c) return
  ratio = Math.min(window.devicePixelRatio || 1, 2)
  c.width = Math.round(window.innerWidth * ratio)
  c.height = Math.round(window.innerHeight * ratio)
}

function frame(now) {
  raf = 0
  ctx.setTransform(1, 0, 0, 1, 0, 0)
  ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)
  ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
  ctx.lineCap = 'round'
  ctx.lineWidth = props.width

  for (let i = bursts.length - 1; i >= 0; i--) {
    const b = bursts[i]
    const p = (now - b.start) / props.duration
    if (p >= 1) {
      bursts.splice(i, 1)
      continue
    }
    const e = easeOut(p)
    const dist = props.radius * e
    const len = props.size * (1 - e)
    ctx.globalAlpha = 1 - Math.max(0, (p - 0.55) / 0.45)
    for (const s of b.sparks) {
      const cos = Math.cos(s.angle)
      const sin = Math.sin(s.angle)
      ctx.strokeStyle = s.color
      ctx.beginPath()
      ctx.moveTo(b.x + cos * dist * s.reach, b.y + sin * dist * s.reach)
      ctx.lineTo(b.x + cos * (dist * s.reach + len), b.y + sin * (dist * s.reach + len))
      ctx.stroke()
    }
  }
  ctx.globalAlpha = 1
  if (bursts.length) raf = requestAnimationFrame(frame)
}

function onPointerDown(e) {
  if (e.button > 0) return
  const target = e.target instanceof Element ? e.target : null
  if (!target || !target.closest(TARGETS) || target.closest(SKIP)) return
  if (target.closest(':disabled, [aria-disabled="true"]')) return
  const offset = Math.floor(Math.random() * MARKER_COLORS.length)
  const sparks = Array.from({ length: props.count }, (_, i) => ({
    angle: (Math.PI * 2 * i) / props.count + (Math.random() - 0.5) * 0.35,
    reach: 0.85 + Math.random() * 0.35,
    color: MARKER_COLORS[(i + offset) % MARKER_COLORS.length]
  }))
  bursts.push({ x: e.clientX, y: e.clientY, start: performance.now(), sparks })
  if (bursts.length > 24) bursts.shift()
  if (!raf) raf = requestAnimationFrame(frame)
}

onMounted(() => {
  if (reducedMotion() || !canvas.value) return
  ctx = canvas.value.getContext('2d')
  resize()
  window.addEventListener('resize', resize)
  document.addEventListener('pointerdown', onPointerDown, { passive: true })
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
  document.removeEventListener('pointerdown', onPointerDown)
})
</script>

<template>
  <canvas ref="canvas" class="click-spark" aria-hidden="true"></canvas>
</template>

<style scoped>
.click-spark {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
  z-index: 80;
}
</style>
