<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'
import { notify } from '../notify'
import { confirmAction } from '../confirm'
import { coffees, dateTime, money } from '../format'
import TallyMarks from '../components/TallyMarks.vue'
import WhiteBoard from '../components/WhiteBoard.vue'
import DrinkOverlay from '../components/DrinkOverlay.vue'

const UNDO_MS = 10 * 60 * 1000
const REFRESH_MS = 20 * 1000
const OVERLAY_MS = 3000

const auth = useAuthStore()
const packages = ref([])
const activity = ref([])
const board = ref([])
const loading = ref(true)
const busy = ref(false)
const now = ref(Date.now())
let clock
let poller
let overlayTimer

const drinkOverlay = ref(false)
const drinkMessage = ref('Ти изпи кафе')
const drinkWarning = ref(false)

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
  if (ok) showDrinkOverlay(drinksToday())
}

function showDrinkOverlay(count) {
  if (count <= 1) drinkMessage.value = 'Ти изпи кафе'
  else if (count === 2) drinkMessage.value = 'Ти изпи второ кафе'
  else drinkMessage.value = 'Много кафета пиеш'
  drinkWarning.value = count > 2
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

async function buy(pkg) {
  const ok = await confirmAction({
    title: `Купи ${pkg.name}`,
    message: `Ще получиш ${coffees(pkg.coffeeCount)} за ${money(pkg.price)}. Остави парите в касичката за кафе.`,
    confirmText: `Купи за ${money(pkg.price)}`
  })
  if (!ok) return
  await run(
    () => api('/me/purchases', { method: 'POST', body: { packageId: pkg.id } }),
    `Добавени ${coffees(pkg.coffeeCount)} към дъската ти.`
  )
}
</script>

<template>
  <div class="home">
    <section class="panel balance" aria-labelledby="balance-title">
      <p id="balance-title" class="greeting">Здравей, {{ auth.user?.displayName }}</p>
      <template v-if="unlimited">
        <TallyMarks :count="0" unlimited />
        <p class="figure"><span class="figure-unit">Имаш безкрайни кафета</span></p>
      </template>
      <template v-else>
        <p class="figure">
          <span class="figure-number hand">{{ balance }}</span>
          <span class="figure-unit">{{ balance === 1 ? 'кафе' : 'кафета' }} на дъската</span>
        </p>
        <TallyMarks :count="balance" />
      </template>
      <p v-if="!loading && !unlimited && balance === 0" class="hint">
        Нямаш налични кафета. Купи пакет, за да отбелязваш изпитите.
      </p>
      <div class="balance-actions">
        <button class="btn btn-primary btn-drink" :disabled="busy || (!unlimited && balance === 0)" @click="drink">
          Изпих кафе
        </button>
        <button v-if="canUndo" class="btn btn-quiet" :disabled="busy" @click="undo">
          Отмени последното
        </button>
      </div>
    </section>

    <section class="panel packages" aria-labelledby="packages-title">
      <h2 id="packages-title">Зареди кафета</h2>
      <p v-if="unlimited" class="hint">
        Не е нужно да купуваш пакети. Отбелязвай изпитите кафета, за да се виждат в статистиката.
      </p>
      <p v-else class="hint">Плащаш предварително и кафетата се добавят към твоята дъска.</p>
      <ul v-if="packages.length && !unlimited" class="package-list">
        <li v-for="p in packages" :key="p.id">
          <div>
            <p class="package-name">{{ p.name }}</p>
            <p class="hint">{{ money(p.price / p.coffeeCount) }} за кафе</p>
          </div>
          <button class="btn" :disabled="busy" @click="buy(p)">{{ money(p.price) }}</button>
        </li>
      </ul>
      <p v-else-if="!loading && !unlimited" class="hint empty">Все още няма пакети. Администраторът трябва да добави поне един.</p>
    </section>

    <WhiteBoard
      class="board-area"
      :entries="board"
      :current-user-id="auth.user?.id ?? null"
      :loading="loading"
    />

    <section class="history" aria-labelledby="history-title">
      <h2 id="history-title">История</h2>
      <ol v-if="activity.length" class="activity">
        <li v-for="a in activity" :key="a.type + a.id" :class="a.type === 'PURCHASE' ? 'is-purchase' : ''">
          <span class="activity-when">{{ dateTime(a.createdAt) }}</span>
          <span class="activity-what">
            {{ a.type === 'PURCHASE' ? `Купен пакет „${a.label}“ за ${money(a.amount)}` : a.label }}
          </span>
          <span class="activity-delta">{{ a.type === 'PURCHASE' ? `+${a.coffees}` : '−1' }}</span>
        </li>
      </ol>
      <p v-else-if="!loading" class="hint">Тук ще се появят покупките и изпитите ти кафета.</p>
    </section>
     <DrinkOverlay
      :show="drinkOverlay"
      :message="drinkMessage"
      :warning="drinkWarning"
      @close="hideDrinkOverlay"
    />
  </div>
</template>

<style scoped>
.home {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 1.25rem;
  align-items: start;
}
.balance { display: grid; gap: 1.1rem; padding: 2rem; }
.greeting { color: var(--muted); font-weight: 500; }
.figure { display: flex; align-items: baseline; gap: 0.75rem; flex-wrap: wrap; }
.figure-number { font-size: clamp(5rem, 14vw, 7.5rem); }
.figure-unit { font-size: var(--step-1); font-weight: 500; }
.balance-actions { display: flex; gap: 0.75rem; align-items: center; flex-wrap: wrap; margin-top: 0.5rem; }
.btn-drink { font-size: var(--step-1); padding: 0.9rem 2rem; border-radius: 10px; }

.packages { display: grid; gap: 0.25rem; }
.package-list { list-style: none; padding: 0; margin: 1rem 0 0; }
.package-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 0;
  border-top: 1px solid var(--rule);
}
.package-name { font-weight: 600; }
.empty { margin-top: 1rem; }

.board-area { grid-column: 1 / -1; margin-top: 0.75rem; }
.history { grid-column: 1 / -1; margin-top: 1rem; }
.activity { list-style: none; padding: 0; margin: 0.75rem 0 0; }
.activity li {
  display: grid;
  grid-template-columns: 9.5rem 1fr auto;
  gap: 1rem;
  padding: 0.6rem 0;
  border-bottom: 1px solid var(--rule);
  font-size: var(--step--1);
}
.activity-when { color: var(--muted); font-variant-numeric: tabular-nums; }
.activity-delta { font-weight: 700; font-variant-numeric: tabular-nums; color: var(--muted); }
.is-purchase .activity-delta { color: var(--green); }

@media (max-width: 760px) {
  .home { grid-template-columns: 1fr; }
  .balance { padding: 1.5rem; }
  .btn-drink { width: 100%; }
  .activity li { grid-template-columns: 1fr auto; }
  .activity-when { grid-column: 1 / -1; }
}
</style>
