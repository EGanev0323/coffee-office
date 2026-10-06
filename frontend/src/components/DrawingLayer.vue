<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { api } from '../api'
import { notify } from '../notify'
import { subscribeBoardEvents } from '../boardEvents'

/*
  Прозрачен canvas върху дъската. Когато е избран маркер (color), може да се рисува.
  color === 'ERASER' е гъбата: линията се рисува с destination-out и изтрива пикселите
  на рисунките под себе си. Чертичките и имената са отделно, под canvas-а, и не се пипат.
  Точките се пазят нормализирани спрямо ширината на дъската ([0..1], y също спрямо ширината),
  за да изглежда рисунката еднакво на телефон и на компютър.
*/
const props = defineProps({
  color: { type: String, default: null } // null = не се рисува, 'ERASER' = гъба
})
const emit = defineEmits(['clearing'])

const ERASER = 'ERASER'
const MIN_STEP = 0.003
const MAX_POINTS = 2000
const MAX_Y = 10 // като DrawingService.MAX_Y – по-нататък сървърът отказва линията

const canvas = ref(null)
const fading = ref(false)

const strokes = new Map() // id -> { color, points } – записаните
const pending = new Set() // пуснати към сървъра, още без отговор
let current = null // линията, която се рисува в момента
let ctx = null
let width = 0
let height = 0
let resizeObserver
let unsubscribe

const round = (v) => Math.round(v * 10000) / 10000
const lineWidth = () => Math.max(2.5, width * 0.0045)
const eraserWidth = () => Math.max(16, width * 0.03)

/** Настройва „писалката“ – маркер с цвят или гъба, която трие. */
function applyPen(color) {
  if (color === ERASER) {
    ctx.globalCompositeOperation = 'destination-out'
    ctx.strokeStyle = '#000'
    ctx.lineWidth = eraserWidth()
  } else {
    ctx.globalCompositeOperation = 'source-over'
    ctx.strokeStyle = color
    ctx.lineWidth = lineWidth()
  }
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
}

function resize() {
  const el = canvas.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const dpr = window.devicePixelRatio || 1
  width = rect.width
  height = rect.height
  el.width = Math.round(width * dpr)
  el.height = Math.round(height * dpr)
  ctx = el.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  redraw()
}

function drawStroke(s) {
  if (!ctx || !s.points.length) return
  const [first, ...rest] = s.points
  applyPen(s.color)
  ctx.beginPath()
  ctx.moveTo(first[0] * width, first[1] * width)
  if (!rest.length) ctx.lineTo(first[0] * width + 0.1, first[1] * width) // точка
  for (const p of rest) ctx.lineTo(p[0] * width, p[1] * width)
  ctx.stroke()
  ctx.globalCompositeOperation = 'source-over'
}

function redraw() {
  if (!ctx) return
  ctx.clearRect(0, 0, width, height)
  strokes.forEach(drawStroke)
  pending.forEach(drawStroke)
  if (current) drawStroke(current)
}

function toPoint(e) {
  const r = canvas.value.getBoundingClientRect()
  return [
    round(Math.min(1, Math.max(0, (e.clientX - r.left) / r.width))),
    round(Math.min(r.height / r.width, MAX_Y, Math.max(0, (e.clientY - r.top) / r.width)))
  ]
}

function onDown(e) {
  if (!props.color || e.button > 0) return
  e.preventDefault()
  canvas.value.setPointerCapture(e.pointerId)
  current = { color: props.color, points: [toPoint(e)] }
  drawStroke(current)
}

function onMove(e) {
  if (!current || current.points.length >= MAX_POINTS) return
  const p = toPoint(e)
  const last = current.points[current.points.length - 1]
  if (Math.hypot(p[0] - last[0], p[1] - last[1]) < MIN_STEP) return
  current.points.push(p)
  // дорисуваме само новото парче – по-бързо от прерисуване на всичко
  applyPen(current.color)
  ctx.beginPath()
  ctx.moveTo(last[0] * width, last[1] * width)
  ctx.lineTo(p[0] * width, p[1] * width)
  ctx.stroke()
  ctx.globalCompositeOperation = 'source-over'
}

async function onUp() {
  if (!current) return
  const stroke = current
  current = null
  pending.add(stroke)
  try {
    const saved = await api('/board/strokes', {
      method: 'POST',
      body: { color: stroke.color, points: stroke.points }
    })
    strokes.set(saved.id, { color: saved.color, points: saved.points })
  } catch (e) {
    notify(e.message, 'error')
  } finally {
    pending.delete(stroke)
    redraw()
  }
}

async function loadStrokes() {
  try {
    const list = await api('/board/strokes')
    strokes.clear()
    list.forEach((s) => strokes.set(s.id, s))
    redraw()
  } catch {
    // ще опитаме пак при следващото свързване
  }
}

/** Изтриване на всички рисунки – гъбата минава през дъската, а рисунките избледняват. */
function clearAnimated() {
  if (!strokes.size && !pending.size) return
  // Трием само линиите отпреди изтриването – нарисуваните през анимацията остават.
  const ids = [...strokes.keys()]
  const sent = [...pending]
  emit('clearing')
  fading.value = true
  setTimeout(() => {
    ids.forEach((id) => strokes.delete(id))
    sent.forEach((s) => pending.delete(s))
    redraw()
    fading.value = false
  }, 900)
}

defineExpose({ clearAnimated })

onMounted(() => {
  resize()
  resizeObserver = new ResizeObserver(resize)
  resizeObserver.observe(canvas.value)
  loadStrokes()
  unsubscribe = subscribeBoardEvents({
    open: loadStrokes, // при (пре)свързване – наваксваме пропуснатото
    stroke: (s) => {
      if (strokes.has(s.id)) return // собствената ни линия вече е тук
      strokes.set(s.id, s)
      drawStroke(s)
    },
    clear: clearAnimated
  })
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  unsubscribe?.()
})
</script>

<template>
  <canvas
    ref="canvas"
    class="drawing-layer"
    :class="{ drawing: !!color, erasing: color === ERASER, fading }"
    aria-hidden="true"
    @pointerdown="onDown"
    @pointermove="onMove"
    @pointerup="onUp"
    @pointercancel="onUp"
  ></canvas>
</template>

<style scoped>
.drawing-layer {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
  transition: opacity 0.8s ease-in;
}
.drawing-layer.drawing {
  pointer-events: auto;
  cursor: crosshair;
  touch-action: none; /* пръстът рисува, вместо да скролва страницата */
}
.drawing-layer.erasing {
  cursor: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='28' height='28'><circle cx='14' cy='14' r='12' fill='white' fill-opacity='0.5' stroke='%232F3437' stroke-width='2'/></svg>") 14 14, cell;
}
.drawing-layer.fading { opacity: 0; }
</style>