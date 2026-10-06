<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../../api'
import { notify } from '../../notify'
import { confirmAction } from '../../confirm'
import { coffees, dateTime, money, monthLabel } from '../../format'

const today = new Date()
const year = ref(today.getFullYear())
const month = ref(today.getMonth() + 1)
const stats = ref(null)
const purchases = ref([])
const loading = ref(false)

const sortKey = ref('balance')
const sortDir = ref(-1)

const isCurrentMonth = computed(
  () => year.value === today.getFullYear() && month.value === today.getMonth() + 1
)
const label = computed(() => monthLabel(year.value, month.value))

const rows = computed(() => {
  if (!stats.value) return []
  const key = sortKey.value
  return [...stats.value.users].sort((a, b) => {
    const val = (u) => (key === 'balance' && u.unlimited ? Infinity : Number(u[key]))
    const av = key === 'displayName' ? a[key] : val(a)
    const bv = key === 'displayName' ? b[key] : val(b)
    if (key === 'displayName') return av.localeCompare(bv, 'bg') * sortDir.value
    if (av === bv) return a.displayName.localeCompare(b.displayName, 'bg')
    return (av < bv ? -1 : 1) * sortDir.value
  })
})

function sortBy(key) {
  if (sortKey.value === key) sortDir.value *= -1
  else {
    sortKey.value = key
    sortDir.value = key === 'displayName' ? 1 : -1
  }
}
function ariaSort(key) {
  if (sortKey.value !== key) return undefined
  return sortDir.value === 1 ? 'ascending' : 'descending'
}

function shift(delta) {
  let m = month.value + delta
  let y = year.value
  if (m < 1) { m = 12; y-- }
  if (m > 12) { m = 1; y++ }
  year.value = y
  month.value = m
  load()
}

async function load() {
  loading.value = true
  try {
    const q = `?year=${year.value}&month=${month.value}`
    const [s, p] = await Promise.all([api(`/admin/stats${q}`), api(`/admin/purchases${q}`)])
    stats.value = s
    purchases.value = p
  } catch (e) {
    notify(e.message, 'error')
  } finally {
    loading.value = false
  }
}

async function removePurchase(p) {
  const ok = await confirmAction({
    title: 'Изтрий покупката',
    message: `${p.userName}: „${p.packageName}“ за ${money(p.amount)}. ${coffees(p.coffeeCount)} ще бъдат махнати от баланса и сумата няма да се брои в отчета.`,
    confirmText: 'Изтрий покупката',
    danger: true
  })
  if (!ok) return
  try {
    await api(`/admin/purchases/${p.id}`, { method: 'DELETE' })
    notify('Покупката е изтрита.')
    await load()
  } catch (e) {
    notify(e.message, 'error')
  }
}

onMounted(load)
</script>

<template>
  <div class="overview">
    <header class="month">
      <button class="btn btn-quiet month-arrow" aria-label="Предишен месец" @click="shift(-1)">‹</button>
      <h1 class="month-name">{{ label }}</h1>
      <button class="btn btn-quiet month-arrow" aria-label="Следващ месец" :disabled="isCurrentMonth" @click="shift(1)">›</button>
    </header>

    <section v-if="stats" class="summary" :aria-busy="loading">
      <p class="collected">
        <span class="collected-sum hand">{{ money(stats.totalCollected) }}</span>
        <span class="collected-text">
          събрани през месеца от {{ stats.purchasesCount }}
          {{ stats.purchasesCount === 1 ? 'покупка' : 'покупки' }}
        </span>
      </p>
      <dl class="facts">
        <div><dt>Продадени кафета</dt><dd>{{ stats.coffeesSold }}</dd></div>
        <div><dt>Изпити кафета</dt><dd>{{ stats.coffeesConsumed }}</dd></div>
        <div><dt>Предплатени, още неизпити (всички колеги без безкрайните)</dt><dd>{{ stats.outstandingCoffees }}</dd></div>
      </dl>
    </section>

    <section v-if="stats" class="panel">
      <h2>Кой колко кафета има</h2>
      <p class="hint">„Налични“ е текущият баланс. Останалите колони са само за {{ label }}.</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th :aria-sort="ariaSort('displayName')"><button class="sort-btn" @click="sortBy('displayName')">Колега</button></th>
              <th class="num" :aria-sort="ariaSort('balance')"><button class="sort-btn" @click="sortBy('balance')">Налични</button></th>
              <th class="num" :aria-sort="ariaSort('bought')"><button class="sort-btn" @click="sortBy('bought')">Купени</button></th>
              <th class="num" :aria-sort="ariaSort('consumed')"><button class="sort-btn" @click="sortBy('consumed')">Изпити</button></th>
              <th class="num" :aria-sort="ariaSort('spent')"><button class="sort-btn" @click="sortBy('spent')">Платени</button></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in rows" :key="u.id" :class="{ 'is-muted': !u.active }">
              <td>
                {{ u.displayName }}
                <span v-if="!u.active" class="badge badge-off">неактивен</span>
              </td>
              <td class="num">
                <span class="balance-cell hand" :aria-label="u.unlimited ? 'безкрайни' : null">{{ u.unlimited ? '∞+1' : u.balance }}</span>
              </td>
              <td class="num">{{ u.bought }}</td>
              <td class="num">{{ u.consumed }}</td>
              <td class="num">{{ money(u.spent) }}</td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="5" class="hint">Няма колеги. Добави ги от раздел „Колеги“.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel">
      <h2>Покупки през месеца</h2>
      <p class="hint">Изтрий покупка, ако е въведена по грешка или парите не са платени.</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Кога</th>
              <th>Колега</th>
              <th>Пакет</th>
              <th class="num">Сума</th>
              <th><span class="visually-hidden">Действия</span></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in purchases" :key="p.id">
              <td>{{ dateTime(p.createdAt) }}</td>
              <td>
                {{ p.userName }}
                <span v-if="p.createdBy && p.createdBy !== p.userName" class="hint">(въведено от {{ p.createdBy }})</span>
              </td>
              <td>{{ p.packageName }}</td>
              <td class="num">{{ money(p.amount) }}</td>
              <td class="num">
                <button class="btn btn-danger btn-small" @click="removePurchase(p)">Изтрий</button>
              </td>
            </tr>
            <tr v-if="!purchases.length">
              <td colspan="5" class="hint">През този месец няма покупки.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.overview { display: grid; gap: 1.5rem; }
.month { display: flex; align-items: center; gap: 0.5rem; }
.month-name { font-size: var(--step-2); text-transform: capitalize; min-width: 10ch; text-align: center; }
.month-arrow { font-size: 1.5rem; line-height: 1; padding: 0.3rem 0.8rem; }

.summary { display: grid; gap: 1.25rem; padding: 0.5rem 0 0.5rem; }
.collected { display: flex; align-items: baseline; gap: 1rem; flex-wrap: wrap; }
.collected-sum { font-size: clamp(4rem, 12vw, 6.5rem); }
.collected-text { font-size: var(--step-1); font-weight: 500; max-width: 24ch; }

.facts { display: flex; flex-wrap: wrap; gap: 0.75rem 2.5rem; margin: 0; }
.facts div { display: grid; gap: 0.1rem; }
.facts dt { color: var(--muted); font-size: var(--step--1); }
.facts dd { margin: 0; font-size: var(--step-1); font-weight: 650; font-variant-numeric: tabular-nums; }

.panel h2 + .hint { margin: 0.25rem 0 0.75rem; }
.balance-cell { font-size: 1.6rem; }
.visually-hidden {
  position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap;
}
</style>
