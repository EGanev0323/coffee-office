<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import TallyMarks from './TallyMarks.vue'
import DrawingLayer from './DrawingLayer.vue'
import SplitText from './fx/SplitText.vue'
import { api } from '../api'
import { notify } from '../notify'
import { confirmAction } from '../confirm'
import { afterTick, gsap, isPhone, reducedMotion } from '../motion'

/*
  Общата дъска в офиса: всеки колега с кафета и неговите чертички.
  Когато нечий баланс намалее, гъбата тръгва от поставката, изтрива последната
  чертичка и се връща обратно.
  Маркерите на поставката са бутони – с тях се рисува по дъската, а гъбата трие рисунките.
*/
const props = defineProps({
  entries: { type: Array, required: true },
  currentUserId: { type: Number, default: null },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['drawing']) // true, докато е избран маркер или гъбата

const MAX_VISIBLE = 30   // над толкова чертички се пише „и още N“
const MAX_ERASE = 5      // най-много толкова чертички се трият с анимация наведнъж
const MOVE_MS = 380
const SCRUB_MS = 500
const ERASER_W = 64
const ERASER_H = 26
const ERASER = 'ERASER'
const MARKERS = [
  { color: '#1E4BAF', name: 'синия' },
  { color: '#B8372A', name: 'червения' },
  { color: '#2B1F18', name: 'черния' }
]

const reduceMotion =
  typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

const frame = ref(null)
const tray = ref(null)
const boardEl = ref(null)
const drawing = ref(null)
const drawColor = ref(null) // избраният маркер; null = не се рисува
// Показаните редове. balance е това, което се вижда; target е истинският баланс от сървъра.
const shown = ref([])
const eraserPos = ref({ left: 0, top: 0 })
const eraserPlaced = ref(false)
const scrubbing = ref(false)
const busy = ref(false) // гъбата е заета с анимация

let running = false
let alive = true
let resizeObserver

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

watch(() => props.entries, sync, { deep: true })

function sync(next) {
  const current = new Map(shown.value.map((r) => [r.id, r]))

  // Колегите остават на дъската и с 0 кафета. Ред изчезва само ако админът
  // скрие или деактивира колегата.
  shown.value = next.map((e) => {
    const row = current.get(e.id)
    if (!row) return { ...e, target: e.balance }

    row.displayName = e.displayName
    row.unlimited = e.unlimited
    row.target = e.balance
    if (e.unlimited || reduceMotion || e.balance >= row.balance) {
      row.balance = e.balance // покупка или без анимация – веднага
    } else if (row.balance - e.balance > MAX_ERASE) {
      row.balance = e.balance + MAX_ERASE // голяма корекция – само последните се трият с гъбата
    }
    return row
  })
  run()
}

async function run() {
  if (running) return
  running = true
  busy.value = true
  try {
    let row
    while (alive && (row = shown.value.find((r) => r.balance > r.target))) {
      await eraseOne(row)
    }
  } finally {
    running = false
    busy.value = false
    if (alive) park()
  }
}

// ---------- рисуване ----------

function toggleMarker(color) {
  drawColor.value = drawColor.value === color ? null : color
}

function toggleEraser() {
  if (busy.value) return
  drawColor.value = drawColor.value === ERASER ? null : ERASER
}

const markerName = (color) => MARKERS.find((m) => m.color === color)?.name ?? ''
/** „със синия“, но „с червения“ – предлогът се удвоява пред с/з. */
const withMarker = (color) => {
  const name = markerName(color)
  return `${/^[сз]/.test(name) ? 'със' : 'с'} ${name} маркер`
}

watch(drawColor, (color) => emit('drawing', !!color))

/*
  На телефон дъската е почти цял екран, а докато се рисува, пръстът не скролва страницата.
  Затова лентата с „Готово“ стои фиксирана над табовете, вместо подсказката под поставката.
*/
const phone = ref(isPhone())
const showBar = computed(() => !!drawColor.value && phone.value)
function onResize() { phone.value = isPhone() }

function barEnter(el, done) {
  if (reducedMotion()) return done()
  gsap.fromTo(el, { y: 24, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: 0.35, ease: 'back.out(1.6)', onComplete: done })
}
function barLeave(el, done) {
  if (reducedMotion()) return afterTick(done)
  gsap.to(el, { y: 24, autoAlpha: 0, duration: 0.2, ease: 'power2.in', onComplete: done })
}

function onKey(e) {
  if (e.key === 'Escape') drawColor.value = null
}

async function askClear() {
  if (busy.value) return
  const ok = await confirmAction({
    title: 'Изтрий рисунките',
    message: 'Всички рисунки ще изчезнат от дъската на всички колеги. Чертичките за кафетата остават.',
    confirmText: 'Изтрий рисунките',
    danger: true
  })
  if (!ok) return
    drawColor.value = null
  try {
    await api('/board/strokes', { method: 'DELETE' })
    drawing.value?.clearAnimated()
  } catch (e) {
    notify(e.message, 'error')
  }
}

/** Гъбата минава на зигзаг през дъската, докато рисунките избледняват. */
async function sweep() {
  if (running || !frame.value || !boardEl.value || reduceMotion) return
  running = true
  busy.value = true
  try {
    const f = frame.value.getBoundingClientRect()
    const b = boardEl.value.getBoundingClientRect()
    const path = [[0.12, 0.3], [0.85, 0.3], [0.85, 0.65], [0.12, 0.65]]
    scrubbing.value = true
    for (const [x, y] of path) {
      eraserPos.value = {
        left: b.left - f.left + b.width * x - ERASER_W / 2,
        top: b.top - f.top + b.height * y - ERASER_H / 2
      }
      await sleep(MOVE_MS)
    }
  } finally {
    scrubbing.value = false
    running = false
    busy.value = false
    if (alive) run() // довършва чакащи чертички и прибира гъбата
  }
}

async function eraseOne(row) {
  await nextTick()
  const target = strokeElement(row)
  if (target) {
    moveTo(target)
    await sleep(MOVE_MS)
  }
  scrubbing.value = true
  row.balance -= 1 // чертичката избледнява, докато гъбата трие
  await sleep(SCRUB_MS)
  scrubbing.value = false
}

function strokeElement(row) {
  const rowEl = frame.value?.querySelector(`[data-board-row="${row.id}"]`)
  if (!rowEl) return null
  if (row.balance > MAX_VISIBLE) return rowEl.querySelector('.tally-more')
  return rowEl.querySelector(`[data-stroke="${row.balance - 1}"]`)
}

function moveTo(el) {
  const f = frame.value.getBoundingClientRect()
  const b = el.getBoundingClientRect()
  eraserPos.value = {
    left: b.left - f.left + b.width / 2 - ERASER_W / 2,
    top: b.top - f.top + b.height / 2 - ERASER_H / 2
  }
}

function park() {
  if (!frame.value || !tray.value) return
  const f = frame.value.getBoundingClientRect()
  const t = tray.value.getBoundingClientRect()
  eraserPos.value = {
    left: t.right - f.left - ERASER_W - 24,
    top: t.top - f.top - ERASER_H / 2
  }
  eraserPlaced.value = true
}

onMounted(async () => {
  sync(props.entries)
  await nextTick()
  park()
  resizeObserver = new ResizeObserver(() => { if (!running) park() })
  resizeObserver.observe(frame.value)
  window.addEventListener('keydown', onKey)
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  alive = false
  resizeObserver?.disconnect()
  window.removeEventListener('keydown', onKey)
  window.removeEventListener('resize', onResize)
  if (drawColor.value) emit('drawing', false)
})
</script>

<template>
  <section ref="frame" class="board-frame" aria-labelledby="board-title">
    <div ref="boardEl" class="board">
      <DrawingLayer ref="drawing" :color="drawColor" @clearing="sweep" />
      <SplitText
        id="board-title"
        tag="h2"
        class="board-title"
        text="Кафета"
        :stagger="0.07"
        :from="{ yPercent: 60, autoAlpha: 0, rotation: -12, scale: 0.6 }"
      />

      <TransitionGroup v-if="shown.length" name="board-row" tag="ul" class="board-rows">
        <li
          v-for="(e, i) in shown"
          :key="e.id"
          :data-board-row="e.id"
          class="board-row"
          :class="{ 'is-me': e.id === currentUserId }"
          :style="{ '--row': Math.min(i, 12) }"
        >
          <span class="board-name">
            {{ e.displayName }}<span v-if="e.id === currentUserId" class="visually-hidden"> (ти)</span>
          </span>
          <TallyMarks :count="e.balance" :max="MAX_VISIBLE" size="sm" :unlimited="e.unlimited" />
          <span class="board-count" :class="{ 'is-zero': !e.unlimited && e.balance === 0 }" aria-hidden="true">
            {{ e.unlimited ? '' : e.balance }}
          </span>
        </li>
      </TransitionGroup>

      <p v-else-if="!loading" class="board-empty">
        Дъската е празна. Админът може да покаже колегите от „Колеги“.
      </p>

      <ul v-else class="board-rows" aria-hidden="true">
        <li v-for="n in 3" :key="n" class="board-row board-row-skeleton">
          <span class="skeleton" :style="{ width: `${9 - n}rem`, height: '1.6rem' }"></span>
          <span class="skeleton" :style="{ width: `${4 + n * 2}rem`, height: '1.9rem' }"></span>
        </li>
      </ul>
    </div>

    <div ref="tray" class="board-tray">
      <button
        v-for="(m, i) in MARKERS"
        :key="m.color"
        type="button"
        class="marker"
        :class="{ selected: drawColor === m.color }"
        :style="{ '--cap': m.color, '--i': i }"
        :aria-pressed="drawColor === m.color"
        :aria-label="`Рисувай с ${m.name} маркер`"
        :title="`Рисувай с ${m.name} маркер`"
        @click="toggleMarker(m.color)"
      ></button>
    </div>

    <button
      type="button"
      class="eraser"
      :class="{ placed: eraserPlaced, scrubbing, busy, selected: drawColor === ERASER }"
      :style="{ left: `${eraserPos.left}px`, top: `${eraserPos.top}px` }"
      :aria-pressed="drawColor === ERASER"
      aria-label="Гъба – изтривай части от рисунките"
      title="Гъба – изтривай части от рисунките"
      @click="toggleEraser"
    >
      <span class="eraser-grip"></span>
      <span class="eraser-felt"></span>
    </button>

    <template v-if="!phone">
      <p v-if="drawColor === ERASER" class="draw-hint" role="status">
        Триеш с гъбата. Плъзни по рисунката, която искаш да махнеш.
        <button type="button" class="btn btn-danger btn-small" @click="askClear">Изтрий всичко</button>
      </p>
      <p v-else-if="drawColor" class="draw-hint" role="status">
        Рисуваш {{ withMarker(drawColor) }}. Натисни го пак или Esc, за да спреш.
      </p>
    </template>

    <Teleport to="body">
      <Transition :css="false" @enter="barEnter" @leave="barLeave">
        <div v-if="showBar" class="draw-bar" role="status">
          <span class="draw-bar-dot" :class="{ 'is-eraser': drawColor === ERASER }" :style="{ '--cap': drawColor }" aria-hidden="true"></span>
          <span class="draw-bar-text">{{ drawColor === ERASER ? 'Гъба' : `Рисуваш ${withMarker(drawColor)}` }}</span>
          <button v-if="drawColor === ERASER" type="button" class="btn btn-danger draw-bar-clear" @click="askClear">Изтрий всичко</button>
          <button type="button" class="btn btn-primary" @click="drawColor = null">Готово</button>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<style scoped>
.board-frame {
  position: relative;
  background: #C3C8CC;
  border-radius: 10px;
  padding: 10px 10px 12px;
}
.board {
  position: relative;
  background: #FDFDFB;
  border-radius: 3px;
  box-shadow: inset 0 0 0 1px #B3B9BE;
  padding: 1.25rem 1.75rem 1.75rem;
}
.board-title {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 2.4rem;
  line-height: 1;
  margin-bottom: 1rem;
}

.board-rows { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.6rem; }
.board-row {
  display: grid;
  grid-template-columns: minmax(8rem, 13rem) 1fr auto;
  align-items: center;
  gap: 1rem;
}
.board-name {
  justify-self: start;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 1.7rem;
  line-height: 1.1;
  overflow-wrap: anywhere;
}
.is-me .board-name {
  text-decoration: underline;
  text-decoration-color: var(--red);
  text-decoration-thickness: 2px;
  text-underline-offset: 5px;
}
.board-count {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 1.5rem;
  color: var(--muted);
  min-width: 2ch;
  text-align: right;
}
.board-count.is-zero { color: var(--red); }
.board-empty {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 1.5rem;
  color: var(--muted);
}

/* поставката с маркерите */
.board-tray {
  --marker-w: 72px;
  --marker-gap: 84px;
  position: relative;
  height: 18px;
  margin: 0 1.5rem;
  background: #A9AFB4;
  border-radius: 0 0 6px 6px;
}
.marker {
  position: absolute;
  top: -13px; /* зоната за натискане е 44 px висока, видимият маркер е в средата ѝ */
  left: calc(1.25rem + var(--i) * var(--marker-gap));
  width: var(--marker-w);
  height: 44px;
  padding: 0;
  border: 0;
  background: none;
  cursor: pointer;
  transition: transform 0.2s ease-out;
}
.marker::before,
.marker::after {
  content: '';
  position: absolute;
  top: 16px;
  height: 10px;
  border-radius: 5px;
}
.marker::before { left: 0; right: 0; background: #E6E7E8; }
.marker::after { left: 0; width: 20px; border-radius: 5px 0 0 5px; background: var(--cap); }
.marker.selected { transform: translateY(-10px) rotate(-5deg); }
.draw-hint {
  margin: 0.6rem 0.5rem 0;
  text-align: center;
  font-size: var(--step--1);
  font-weight: 500;
}

.draw-hint .btn { margin-left: 0.5rem; vertical-align: middle; }

/* гъбата */
.eraser {
  position: absolute;
  width: 64px;
  height: 26px;
  z-index: 2;
  padding: 0;
  border: 0;
  background: none;
  cursor: pointer;
  visibility: hidden;
  transition: left 0.38s ease-in-out, top 0.38s ease-in-out;
}
.eraser.placed { visibility: visible; }
/* по-голяма невидима зона за пръста около гъбата */
.eraser::before { content: ''; position: absolute; inset: -10px -6px -8px; }
.eraser.busy { pointer-events: none; }
.eraser.selected { transform: translateY(-10px) rotate(-5deg); }
.eraser-grip { display: block; height: 16px; background: #2F3437; border-radius: 5px 5px 0 0; }
.eraser-felt { display: block; height: 10px; background: #D8CDB9; border-radius: 0 0 3px 3px; }
.eraser.scrubbing { animation: scrub 0.5s ease-in-out; }
@keyframes scrub {
  0%, 100% { transform: translateX(0) rotate(-4deg); }
  25% { transform: translateX(-9px) rotate(-4deg); }
  75% { transform: translateX(9px) rotate(-4deg); }
}

/* редовете се „изписват“ един след друг: името се показва отляво надясно като с маркер */
.board-row-enter-active {
  transition: opacity 0.4s ease-out, transform 0.5s cubic-bezier(0.2, 1.3, 0.4, 1);
  transition-delay: calc(var(--row, 0) * 70ms);
}
.board-row-enter-active .board-name {
  transition: clip-path 0.55s cubic-bezier(0.6, 0, 0.3, 1);
  transition-delay: calc(var(--row, 0) * 70ms + 80ms);
}
.board-row-enter-from { opacity: 0; transform: translateX(-14px); }
.board-row-enter-from .board-name { clip-path: inset(0 100% 0 0); }
.board-row-leave-active { transition: opacity 0.6s ease-in; }
.board-row-leave-to { opacity: 0; }
.board-row-move { transition: transform 0.5s cubic-bezier(0.3, 1.2, 0.5, 1); }

/* лек „маркерен“ наклон на името при посочване */
.board-name { display: inline-block; transition: transform 0.3s cubic-bezier(0.3, 1.6, 0.5, 1); }
@media (hover: hover) {
  .board-row:hover .board-name { transform: rotate(-1.5deg) translateX(3px); }
  .marker:not(.selected):hover { transform: translateY(-2px); }
  .eraser:not(.busy):not(.selected):hover { transform: translateY(-2px); }
}
.board-row-skeleton .skeleton { max-width: 100%; }

/* лентата при рисуване на телефон (телепортирана в body, затова :global) */
:global(.draw-bar) {
  position: fixed;
  left: var(--gutter-l);
  right: var(--gutter-r);
  bottom: calc(var(--tabbar-h) + 0.75rem + var(--safe-bottom));
  z-index: 44;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  min-height: var(--fab-h);
  padding: 0.4rem 0.4rem 0.4rem 0.9rem;
  border-radius: 16px;
  background: var(--surface);
  box-shadow: 0 0 0 1px var(--rule), 0 14px 30px -12px rgba(43, 31, 24, 0.45);
}
:global(.draw-bar-dot) {
  flex: none;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--cap);
}
:global(.draw-bar-dot.is-eraser) { border-radius: 3px; background: #D8CDB9; box-shadow: inset 0 -5px 0 #2F3437; }
:global(.draw-bar-text) { flex: 1 1 auto; min-width: 0; font-size: var(--step--1); font-weight: 600; line-height: 1.3; }
:global(.draw-bar .btn) { flex: none; padding-inline: 0.9rem; }

.visually-hidden {
  position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap;
}

@media (max-width: 560px) {
  .board-frame { padding: 8px 8px 10px; }
  .board { padding: 1rem 1.1rem 1.25rem; }
  .board-tray { margin: 0 0.75rem; --marker-w: 48px; --marker-gap: 56px; }
  .board-row { grid-template-columns: 1fr auto; row-gap: 0.2rem; }
  .board-row :deep(.tally) { grid-column: 1 / -1; grid-row: 2; }
}
</style>