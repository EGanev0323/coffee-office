<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'
import { notify } from '../notify'
import { confirmAction } from '../confirm'
import { coffees, dayLabel, money, time } from '../format'
import TallyMarks from '../components/TallyMarks.vue'
import WhiteBoard from '../components/WhiteBoard.vue'
import DrinkOverlay from '../components/DrinkOverlay.vue'
import QuantityStepper from '../components/QuantityStepper.vue'
import SplitText from '../components/fx/SplitText.vue'
import CountUp from '../components/fx/CountUp.vue'
import ShinyText from '../components/fx/ShinyText.vue'
import SteamCup from '../components/fx/SteamCup.vue'
import { burst } from '../motion/burst'
import { afterTick, gsap, haptic, reducedMotion } from '../motion'

const UNDO_MS = 10 * 60 * 1000
const REFRESH_MS = 20 * 1000
const OVERLAY_MS = 3000
const HISTORY_STEP = 10 // колко реда от историята се показват наведнъж

const auth = useAuthStore()
const packages = ref([])
const quantities = reactive({}) // packageId -> колко пъти да се купи пакетът
const qty = (pkg) => quantities[pkg.id] ?? 1
const activity = ref([])
const board = ref([])
const loading = ref(true)
const busy = ref(false)
const now = ref(Date.now())
let clock
let poller
let overlayTimer

const drinkOverlay = ref(false)
const drinkCount = ref(1) // кое кафе за днес – избира надписа на плаката

/** Колко кафета си изпил днес – от историята, която се опреснява след всяко кафе. */
function drinksToday() {
  const today = new Date().toDateString()
  return activity.value.filter(
    (a) => a.type === 'CONSUMPTION' && new Date(a.createdAt).toDateString() === today
  ).length
}

const balance = computed(() => auth.user?.balance ?? 0)
const unlimited = computed(() => !!auth.user?.unlimited)
const canUndo = computed(() => {
  const last = activity.value[0]
  return !!last && last.type === 'CONSUMPTION' && now.value - new Date(last.createdAt).getTime() < UNDO_MS
})

/*
  Плаващ бутон „Изпих кафе“ над лентата с табове – показва се, когато големият бутон е скролнат извън екрана,
  за да може да се отбележи кафе с палеца от всяко място на страницата.
*/
const drinkBtn = ref(null)
const drinkBtnVisible = ref(true)
const canDrink = computed(() => !loading.value && (unlimited.value || balance.value > 0))
const drawing = ref(false) // докато се рисува по дъската, на мястото на бутона е лентата „Готово“
const showFab = computed(() => canDrink.value && !drinkBtnVisible.value && !drinkOverlay.value && !drawing.value)
let drinkObserver

// Toast-овете се вдигат над плаващия бутон / лентата за рисуване, за да не ги закриват.
watch(
  () => showFab.value || drawing.value,
  (raised) => document.body.classList.toggle('has-fab', raised),
  { immediate: true }
)
onUnmounted(() => document.body.classList.remove('has-fab'))

/* Историята по дни: първо HISTORY_STEP реда, „Покажи още“ добавя следващите. */
const historyLimit = ref(HISTORY_STEP)
const historyRows = computed(() => {
  const out = []
  let lastDay = null
  for (const a of activity.value.slice(0, historyLimit.value)) {
    const day = new Date(a.createdAt).toDateString()
    if (day !== lastDay) {
      out.push({ key: `day-${day}`, day: dayLabel(a.createdAt) })
      lastDay = day
    }
    out.push({ key: a.type + a.id, item: a })
  }
  return out
})

function fabEnter(el, done) {
  if (reducedMotion()) return done()
  gsap.fromTo(el, { y: 30, scale: 0.8, autoAlpha: 0 }, { y: 0, scale: 1, autoAlpha: 1, duration: 0.45, ease: 'back.out(2)', onComplete: done })
}
function fabLeave(el, done) {
  if (reducedMotion()) return afterTick(done)
  gsap.to(el, { y: 30, scale: 0.85, autoAlpha: 0, duration: 0.2, ease: 'power2.in', onComplete: done })
}

onMounted(() => {
  const target = drinkBtn.value
  if (target && 'IntersectionObserver' in window) {
    drinkObserver = new IntersectionObserver(([entry]) => { drinkBtnVisible.value = entry.isIntersecting }, { threshold: 0 })
    drinkObserver.observe(target)
  }
})
onUnmounted(() => drinkObserver?.disconnect())

onMounted(() => {
  clock = setInterval(() => { now.value = Date.now() }, 15000)
  // Дъската се опреснява сама, за да се виждат и кафетата на колегите.
  poller = setInterval(() => {
    if (document.visibilityState === 'visible') refreshShared()
  }, REFRESH_MS)
  document.addEventListener('visibilitychange', onVisible)
  load()
})
onUnmounted(() => {
  clearInterval(clock)
  clearInterval(poller)
  clearTimeout(overlayTimer)
  document.removeEventListener('visibilitychange', onVisible)
})

function onVisible() {
  if (document.visibilityState === 'visible') refreshShared()
}

async function refreshShared() {
  try {
    const [me, b] = await Promise.all([api('/me'), api('/board')])
    auth.user = me
    board.value = b
  } catch {
    // Тихо – ще опитаме пак при следващото опресняване.
  }
}

async function load() {
  try {
    const [me, pk, act, b] = await Promise.all([
      api('/me'), api('/packages'), api('/me/activity'), api('/board')
    ])
    auth.user = me
    packages.value = pk
    activity.value = act
    board.value = b
  } catch (e) {
    notify(e.message, 'error')
  } finally {
    loading.value = false
  }
}

async function run(action, successMessage) {
  if (busy.value) return
  busy.value = true
  try {
    auth.user = await action()
    const [act, b] = await Promise.all([api('/me/activity'), api('/board')])
    activity.value = act
    board.value = b
    now.value = Date.now()
    if (successMessage) notify(successMessage)
    return true
  } catch (e) {
    notify(e.message, 'error')
    return false
  } finally {
    busy.value = false
  }
}

async function drink() {
  const ok = await run(() => api('/me/consumptions', { method: 'POST' }))
  if (ok) {
    haptic([18, 60, 18])
    showDrinkOverlay(drinksToday())
  }
}

function showDrinkOverlay(count) {
  drinkCount.value = count
  drinkOverlay.value = true
  clearTimeout(overlayTimer)
  overlayTimer = setTimeout(hideDrinkOverlay, OVERLAY_MS)
}

function hideDrinkOverlay() {
  clearTimeout(overlayTimer)
  drinkOverlay.value = false
}

function undo() {
  return run(() => api('/me/consumptions/last', { method: 'DELETE' }), 'Последното кафе е върнато на дъската.')
}

async function buy(pkg, event) {
  const button = event?.currentTarget
  const quantity = qty(pkg)
  const total = pkg.coffeeCount * quantity
  const price = pkg.price * quantity
  const ok = await confirmAction({
    title: quantity > 1 ? `Купи ${quantity} × ${pkg.name}` : `Купи ${pkg.name}`,
    message: `Ще получиш ${coffees(total)} за ${money(price)}. Остави парите в касичката за кафе.`,
    confirmText: `Купи за ${money(price)}`
  })
  if (!ok) return
  const done = await run(
    () => api('/me/purchases', { method: 'POST', body: { packageId: pkg.id, quantity } }),
    `Добавени ${coffees(total)} към дъската ти.`
  )
  if (done) {
    quantities[pkg.id] = 1
    haptic(25)
    burst(button, { kind: 'strokes', count: Math.min(8 + total, 26) }) // нови чертички за дъската
  }
}

/* Нов ред в историята изплува отгоре и за миг светва. */
function historyEnter(el, done) {
  if (reducedMotion()) return done()
  gsap
    .timeline({ onComplete: done })
    .from(el, { y: -16, autoAlpha: 0, duration: 0.45, ease: 'back.out(1.6)' })
    .fromTo(el, { backgroundColor: 'rgba(241, 230, 214, 1)' }, { backgroundColor: 'rgba(241, 230, 214, 0)', duration: 1.4, ease: 'power1.out', clearProps: 'backgroundColor' }, 0)
}
function historyLeave(el, done) {
  if (reducedMotion()) return afterTick(done)
  gsap.to(el, { x: 30, autoAlpha: 0, duration: 0.3, ease: 'power2.in', onComplete: done })
}
</script>

<template>
  <div class="home">
    <section v-spotlight class="panel balance" aria-labelledby="balance-title">
      <SteamCup class="balance-cup" :size="76" :delay="0.4" />
      <SplitText
        id="balance-title"
        class="greeting"
        split="words"
        :text="`Здравей, ${auth.user?.displayName ?? ''}`"
        :stagger="0.08"
        :delay="0.15"
      />
      <template v-if="unlimited">
        <TallyMarks :count="0" unlimited />
        <p class="figure"><span class="figure-unit">Имаш безкрайни кафета</span></p>
      </template>
      <template v-else-if="!auth.user">
        <p class="figure" aria-hidden="true">
          <span class="skeleton figure-skeleton"></span>
        </p>
        <span class="skeleton tally-skeleton" aria-hidden="true"></span>
      </template>
      <template v-else>
        <p class="figure">
          <CountUp class="figure-number hand" :value="balance" :duration="1.4" :delay="0.2" />
          <span class="figure-unit">{{ balance === 1 ? 'кафе' : 'кафета' }} на дъската</span>
        </p>
        <TallyMarks :count="balance" />
      </template>
      <p v-if="!loading && !unlimited && balance === 0" class="hint">
        Нямаш налични кафета. Купи пакет, за да отбелязваш изпитите.
      </p>
      <div class="balance-actions">
        <button
          v-magnet="{ strength: 0.25, padding: 28 }"
          ref="drinkBtn"
          class="btn btn-primary btn-drink"
          :disabled="busy || (!unlimited && balance === 0)"
          @click="drink"
        >
          <ShinyText base="#D9E3FA" :disabled="busy || (!unlimited && balance === 0)">Изпих кафе</ShinyText>
        </button>
        <Transition name="undo-pop">
          <button v-if="canUndo" class="btn btn-quiet" :disabled="busy" @click="undo">
            Отмени последното
          </button>
        </Transition>
      </div>
    </section>

    <section v-spotlight v-reveal="{ delay: 0.12, y: 36 }" class="panel packages" aria-labelledby="packages-title">
      <h2 id="packages-title">Зареди кафета</h2>
      <p v-if="unlimited" class="hint">
        Не е нужно да купуваш пакети. Отбелязвай изпитите кафета, за да се виждат в статистиката.
      </p>
      <p v-else class="hint">Плащаш предварително и кафетата се добавят към твоята дъска.</p>
      <ul v-if="packages.length && !unlimited" v-reveal="{ children: 'li', delay: 0.25, x: 24, y: 0 }" class="package-list">
        <li v-for="p in packages" :key="p.id">
          <div class="package-info">
            <p class="package-name">{{ p.name }}</p>
            <p class="hint">
              <template v-if="qty(p) > 1">{{ coffees(p.coffeeCount * qty(p)) }} · </template>{{ money(p.price / p.coffeeCount) }} за кафе
            </p>
          </div>
          <div class="package-buy">
            <QuantityStepper
              :model-value="qty(p)"
              :disabled="busy"
              :label="`Брой ${p.name}`"
              @update:model-value="(v) => (quantities[p.id] = v)"
            />
            <button v-magnet="{ strength: 0.2, padding: 16 }" class="btn package-price" :disabled="busy" @click="buy(p, $event)">
              {{ money(p.price * qty(p)) }}
            </button>
          </div>
        </li>
      </ul>
      <ul v-else-if="loading" class="package-list" aria-hidden="true">
        <li v-for="n in 2" :key="n">
          <div class="package-info">
            <span class="skeleton" style="width: 7rem; height: 1.1rem"></span>
            <span class="skeleton" style="width: 9rem; height: 0.9rem; margin-top: 0.4rem"></span>
          </div>
          <span class="skeleton" style="width: 13rem; height: 2.75rem"></span>
        </li>
      </ul>
      <p v-else-if="!unlimited" class="hint empty">Все още няма пакети. Администраторът трябва да добави поне един.</p>
    </section>

    <WhiteBoard
      v-reveal="{ y: 48, duration: 1 }"
      class="board-area"
      :entries="board"
      :current-user-id="auth.user?.id ?? null"
      :loading="loading"
      @drawing="(on) => (drawing = on)"
    />

    <section v-reveal class="history" aria-labelledby="history-title">
      <h2 id="history-title">История</h2>
      <TransitionGroup
        v-if="activity.length"
        v-reveal="{ children: 'li', stagger: 0.04, y: 16 }"
        tag="ol"
        class="activity"
        :css="false"
        @enter="historyEnter"
        @leave="historyLeave"
      >
        <li
          v-for="r in historyRows"
          :key="r.key"
          :class="r.day ? 'activity-day' : r.item.type === 'PURCHASE' ? 'is-purchase' : ''"
        >
          <template v-if="r.day">{{ r.day }}</template>
          <template v-else>
            <span class="activity-when">{{ time(r.item.createdAt) }}</span>
            <span class="activity-what">
              {{ r.item.type === 'PURCHASE' ? `Купен пакет „${r.item.label}“${r.item.quantity > 1 ? ` × ${r.item.quantity}` : ''} за ${money(r.item.amount)}` : r.item.label }}
            </span>
            <span class="activity-delta">{{ r.item.type === 'PURCHASE' ? `+${r.item.coffees}` : '−1' }}</span>
          </template>
        </li>
      </TransitionGroup>
      <button
        v-if="activity.length > historyLimit"
        type="button"
        class="btn btn-quiet history-more"
        @click="historyLimit += HISTORY_STEP"
      >Покажи още</button>
      <p v-else-if="!loading" class="hint">Тук ще се появят покупките и изпитите ти кафета.</p>
    </section>
    <Teleport to="body">
      <Transition :css="false" @enter="fabEnter" @leave="fabLeave">
        <button v-if="showFab" type="button" class="drink-fab" :disabled="busy" @click="drink">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4.5 9h12v5.5a5 5 0 0 1-5 5h-2a5 5 0 0 1-5-5z" />
            <path d="M16.5 10.5h1.2a2.6 2.6 0 0 1 0 5.2h-1.5" />
          </svg>
          Изпих кафе
          <span v-if="!unlimited" class="drink-fab-count" :aria-label="`остават ${balance}`">{{ balance }}</span>
        </button>
      </Transition>
    </Teleport>

    <DrinkOverlay
      :show="drinkOverlay"
      :count="drinkCount"
      @close="hideDrinkOverlay"
    />
  </div>
</template>

<style scoped>
.home {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  grid-template-areas:
    "balance packages"
    "board board"
    "history history";
  gap: 1.25rem;
  align-items: start;
}
.balance { grid-area: balance; }
.packages { grid-area: packages; }
.balance { position: relative; display: grid; gap: 1.1rem; padding: 2rem; overflow: hidden; }
.balance-cup { position: absolute; top: 1.6rem; right: 1.4rem; }
.greeting { color: var(--muted); font-weight: 500; }
.figure { display: flex; align-items: baseline; gap: 0.75rem; flex-wrap: wrap; }
.figure-number { font-size: clamp(5rem, 14vw, 7.5rem); }
.figure-unit { font-size: var(--step-1); font-weight: 500; }
.balance-actions { display: flex; gap: 0.75rem; align-items: center; flex-wrap: wrap; margin-top: 0.5rem; }
.btn-drink {
  font-size: var(--step-1);
  padding: 0.9rem 2rem;
  border-radius: 10px;
  box-shadow: 0 10px 26px -12px rgba(30, 75, 175, 0.7);
}
.btn-drink:disabled { box-shadow: none; }

.undo-pop-enter-active { transition: opacity 0.3s ease, transform 0.4s cubic-bezier(0.2, 1.5, 0.4, 1); }
.undo-pop-leave-active { transition: opacity 0.2s ease, transform 0.2s ease-in; }
.undo-pop-enter-from, .undo-pop-leave-to { opacity: 0; transform: translateX(-10px) scale(0.9); }

.packages { display: grid; gap: 0.25rem; }
.package-list { list-style: none; padding: 0; margin: 1rem 0 0; }
.package-list li {
  transition: background-color 0.25s ease, padding 0.25s ease;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 0;
  border-top: 1px solid var(--rule);
}
.package-name { font-weight: 600; }
/* текстът се пренася в своята колона, а бутоните остават на реда (на много тесен екран слизат отдолу) */
.package-list li { flex-wrap: wrap; row-gap: 0.6rem; }
.package-info { flex: 1 1 6rem; min-width: 0; }
.package-buy { display: flex; align-items: center; gap: 0.5rem; margin-left: auto; }
.package-price { min-width: 5.5rem; font-variant-numeric: tabular-nums; }
.empty { margin-top: 1rem; }

.board-area { grid-area: board; margin-top: 0.75rem; }
.history { grid-area: history; margin-top: 1rem; }
.figure-skeleton { width: 5rem; height: 5rem; border-radius: 12px; }
.tally-skeleton { width: 12rem; height: 42px; }
.activity { list-style: none; padding: 0; margin: 0.75rem 0 0; }
.activity li {
  display: grid;
  grid-template-columns: 3.5rem 1fr auto;
  gap: 1rem;
  padding: 0.6rem 0;
  border-bottom: 1px solid var(--rule);
  font-size: var(--step--1);
}
.activity li.activity-day {
  display: block;
  padding: 1rem 0 0.35rem;
  border-bottom: 0;
  color: var(--muted);
  font-size: 0.8125rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}
.activity li.activity-day:first-child { padding-top: 0.25rem; }
.activity-when { color: var(--muted); font-variant-numeric: tabular-nums; }
.history-more { margin-top: 0.5rem; width: 100%; }
.activity-delta { font-weight: 700; font-variant-numeric: tabular-nums; color: var(--muted); }
.is-purchase .activity-delta { color: var(--green); }

/* плаващият бутон (телепортиран в body, затова :global) */
:global(.drink-fab) {
  position: fixed;
  left: 50%;
  bottom: calc(var(--tabbar-h) + 0.75rem + var(--safe-bottom));
  translate: -50% 0;
  z-index: 44;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-height: 52px;
  padding: 0 0.9rem 0 1.1rem;
  border: 0;
  border-radius: 999px;
  background: var(--marker);
  color: #fff;
  font: inherit;
  font-weight: 650;
  font-size: var(--step-0);
  box-shadow: 0 12px 28px -10px rgba(30, 75, 175, 0.75), 0 2px 6px rgba(43, 31, 24, 0.15);
  cursor: pointer;
  white-space: nowrap;
}
:global(.drink-fab:active) { scale: 0.95; }
:global(.drink-fab:disabled) { opacity: 0.6; }
:global(.drink-fab svg) {
  width: 22px;
  height: 22px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}
:global(.drink-fab-count) {
  min-width: 1.9rem;
  height: 1.9rem;
  display: grid;
  place-items: center;
  padding: 0 0.4rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
  font-family: var(--font-hand);
  font-size: 1.35rem;
  line-height: 1;
}

@media (hover: hover) {
  .package-list li:hover { background: var(--board); padding-inline: 0.6rem; }
}

@media (max-width: 760px) {
  /* на телефон дъската е веднага след баланса – пакети се купуват рядко */
  .home {
    grid-template-columns: minmax(0, 1fr);
    grid-template-areas: "balance" "board" "packages" "history";
  }
  .board-area { margin-top: 0; }
  .balance { padding: 1.5rem; }
  .balance-cup { width: 58px; height: 58px; top: 1.2rem; right: 1rem; }
  .btn-drink { width: 100%; min-height: 60px; font-size: 1.25rem; }
  /* на телефон: какво е станало отгоре, часът по-дребно отдолу, промяната вдясно */
  .activity li {
    grid-template-columns: 1fr auto;
    grid-template-areas: "what delta" "when delta";
    row-gap: 0.1rem;
    padding: 0.7rem 0;
  }
  .activity-what { grid-area: what; font-size: var(--step-0); }
  .activity-when { grid-area: when; font-size: 0.8125rem; }
  .activity-delta { grid-area: delta; align-self: center; font-size: var(--step-0); }
}
</style>
