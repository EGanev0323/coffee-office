<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../../api'
import { notify } from '../../notify'
import { confirmAction } from '../../confirm'
import { coffees, dateTime, money, monthLabel } from '../../format'
import CountUp from '../../components/fx/CountUp.vue'
import SplitText from '../../components/fx/SplitText.vue'

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
    message: `${p.userName}: „${p.packageName}“${p.quantity > 1 ? ` × ${p.quantity}` : ''} за ${money(p.amount)}. ${coffees(p.coffeeCount)} ще бъдат махнати от баланса и сумата няма да се брои в отчета.`,
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
      <SplitText tag="h1" class="month-name" :text="label" :stagger="0.025" :from="{ yPercent: 70, autoAlpha: 0 }" />
      <button class="btn btn-quiet month-arrow" aria-label="Следващ месец" :disabled="isCurrentMonth" @click="shift(1)">›</button>
    </header>

    <section v-if="stats" v-reveal="{ children: '.collected, .facts > div', stagger: 0.08 }" class="summary" :aria-busy="loading">
      <p class="collected">
        <CountUp class="collected-sum hand" :value="Number(stats.totalCollected)" :format="money" :duration="1.6" />
        <span class="collected-text">
          събрани през месеца от {{ stats.purchasesCount }}
          {{ stats.purchasesCount === 1 ? 'покупка' : 'покупки' }}
        </span>
      </p>
      <dl class="facts">
        <div><dt>Продадени кафета</dt><dd><CountUp :value="stats.coffeesSold" :delay="0.15" /></dd></div>
        <div><dt>Изпити кафета</dt><dd><CountUp :value="stats.coffeesConsumed" :delay="0.25" /></dd></div>
        <div>
          <dt>Предплатени, неизпити <span class="fact-note">всички колеги без безкрайните</span></dt>
          <dd><CountUp :value="stats.outstandingCoffees" :delay="0.35" /></dd>
        </div>
      </dl>
    </section>

    <section v-if="stats" v-spotlight v-reveal="{ delay: 0.1 }" class="panel">
      <h2>Кой колко кафета има</h2>
      <p class="hint">„Налични“ е текущият баланс. Останалите колони са само за {{ label }}.</p>
      <div class="table-wrap" data-lenis-prevent>
        <table class="ptable">
          <thead>
            <tr>
              <th :aria-sort="ariaSort('displayName')"><button class="sort-btn" @click="sortBy('displayName')">Колега</button></th>
              <th class="num" :aria-sort="ariaSort('balance')"><button class="sort-btn" @click="sortBy('balance')">Налични</button></th>
              <th class="num col-extra" :aria-sort="ariaSort('bought')"><button class="sort-btn" @click="sortBy('bought')">Купени</button></th>
              <th class="num col-extra" :aria-sort="ariaSort('consumed')"><button class="sort-btn" @click="sortBy('consumed')">Изпити</button></th>
              <th class="num col-extra" :aria-sort="ariaSort('spent')"><button class="sort-btn" @click="sortBy('spent')">Платени</button></th>
            </tr>
          </thead>
          <TransitionGroup v-reveal="{ children: 'tr', stagger: 0.04, y: 12 }" tag="tbody" name="row-sort">
            <tr v-for="u in rows" :key="u.id" :class="{ 'is-muted': !u.active }">
              <td>
                {{ u.displayName }}
                <span v-if="!u.active" class="badge badge-off">неактивен</span>
                <span class="row-meta">купени {{ u.bought }} · изпити {{ u.consumed }} · {{ money(u.spent) }}</span>
              </td>
              <td class="num">
                <span class="balance-cell hand" :aria-label="u.unlimited ? 'безкрайни' : null">{{ u.unlimited ? '∞+1' : u.balance }}</span>
              </td>
              <td class="num col-extra">{{ u.bought }}</td>
              <td class="num col-extra">{{ u.consumed }}</td>
              <td class="num col-extra">{{ money(u.spent) }}</td>
            </tr>
            <tr v-if="!rows.length" key="empty">
              <td colspan="5" class="hint">Няма колеги. Добави ги от раздел „Колеги“.</td>
            </tr>
          </TransitionGroup>
        </table>
      </div>
    </section>

    <section v-spotlight v-reveal="{ delay: 0.15 }" class="panel">
      <h2>Покупки през месеца</h2>
      <p class="hint">Изтрий покупка, ако е въведена по грешка или парите не са платени.</p>
      <div class="table-wrap" data-lenis-prevent>
        <table class="ptable">
          <thead>
            <tr>
              <th class="col-extra">Кога</th>
              <th>Колега</th>
              <th class="col-extra">Пакет</th>
              <th class="num">Сума</th>
              <th><span class="visually-hidden">Действия</span></th>
            </tr>
          </thead>
          <TransitionGroup tag="tbody" name="row-fade">
            <tr v-for="p in purchases" :key="p.id">
              <td class="col-extra">{{ dateTime(p.createdAt) }}</td>
              <td>
                {{ p.userName }}
                <span v-if="p.createdBy && p.createdBy !== p.userName" class="hint">(въведено от {{ p.createdBy }})</span>
                <span class="row-meta">{{ p.packageName }}<template v-if="p.quantity > 1"> × {{ p.quantity }}</template> · {{ dateTime(p.createdAt) }}</span>
              </td>
              <td class="col-extra">{{ p.packageName }}<template v-if="p.quantity > 1"> × {{ p.quantity }}</template></td>
              <td class="num">{{ money(p.amount) }}</td>
              <td class="num">
                <button class="btn btn-danger btn-small" :aria-label="`Изтрий покупката на ${p.userName}`" @click="removePurchase(p)">Изтрий</button>
              </td>
            </tr>
            <tr v-if="!purchases.length" key="empty">
              <td colspan="5" class="hint">През този месец няма покупки.</td>
            </tr>
          </TransitionGroup>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.overview { display: grid; grid-template-columns: minmax(0, 1fr); gap: 1.5rem; }
.month { display: flex; align-items: center; gap: 0.5rem; }
.month-name { font-size: var(--step-2); text-transform: capitalize; min-width: 10ch; text-align: center; }
.month-arrow { font-size: 1.5rem; line-height: 1; padding: 0.3rem 0.8rem; }

.summary { display: grid; gap: 1.25rem; padding: 0.5rem 0 0.5rem; }
.collected { display: flex; align-items: baseline; gap: 1rem; flex-wrap: wrap; }
.collected-sum { font-size: clamp(4rem, 12vw, 6.5rem); }
.collected-text { font-size: var(--step-1); font-weight: 500; max-width: 24ch; }

/* три еднакви плочки – на телефон не се пренасят на различни места */
.facts { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0.75rem; margin: 0; max-width: 640px; }
.facts div {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.35rem;
  padding: 0.75rem 0.9rem;
  background: var(--surface);
  border: 1px solid var(--rule);
  border-radius: 12px;
}
.facts dt { color: var(--muted); font-size: var(--step--1); line-height: 1.3; }
.fact-note { display: block; font-size: 0.75rem; opacity: 0.85; }
.facts dd { margin: 0; font-size: var(--step-2); font-weight: 650; line-height: 1; font-variant-numeric: tabular-nums; }
@media (max-width: 380px) {
  .facts { grid-template-columns: 1fr 1fr; }
  .facts div:last-child { grid-column: 1 / -1; }
}

.panel h2 + .hint { margin: 0.25rem 0 0.75rem; }
.balance-cell { font-size: 1.6rem; }
.month-arrow { transition: transform 0.25s cubic-bezier(0.3, 1.6, 0.5, 1), background-color 0.2s ease; }
@media (hover: hover) {
  .month-arrow:hover:not(:disabled) { transform: scale(1.15); }
}

/* сортиране: редовете се разместват плавно */
.row-sort-move { transition: transform 0.45s cubic-bezier(0.3, 1.1, 0.4, 1); }
.row-fade-enter-active, .row-sort-enter-active { transition: opacity 0.35s ease, transform 0.35s ease; }
.row-fade-leave-active, .row-sort-leave-active { transition: opacity 0.25s ease; }
.row-fade-enter-from, .row-sort-enter-from { opacity: 0; transform: translateY(8px); }
.row-fade-leave-to, .row-sort-leave-to { opacity: 0; }
.visually-hidden {
  position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap;
}
</style>
