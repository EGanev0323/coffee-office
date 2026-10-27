<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../../api'
import { notify } from '../../notify'
import { money } from '../../format'
import { dialogIn, dialogOut, lockScroll } from '../../motion'

const packages = ref([])
const loading = ref(true)
const editingId = ref(null)
const error = ref('')
const dialog = ref(null)
const open = ref(false) // формата се рисува след showModal – така фокусът е на листа, а не на полето (без клавиатура)
const busy = ref(false)

const empty = () => ({ name: '', coffeeCount: 5, price: 2, active: true, sortOrder: 0 })
const form = reactive(empty())

const perCoffee = computed(() =>
  form.coffeeCount > 0 && form.price >= 0 ? money(form.price / form.coffeeCount) : null
)

async function load() {
  try {
    packages.value = await api('/admin/packages')
  } catch (e) {
    notify(e.message, 'error')
  } finally {
    loading.value = false
  }
}

/* Новият или промененият пакет се попълва в лист отдолу (на компютър – диалог), както при колегите. */
function openEditor(p = null) {
  editingId.value = p?.id ?? null
  Object.assign(form, p
    ? { name: p.name, coffeeCount: p.coffeeCount, price: p.price, active: p.active, sortOrder: p.sortOrder }
    : empty())
  error.value = ''
  open.value = true
  dialog.value.showModal()
  lockScroll(true)
  dialogIn(dialog.value)
}

function closeEditor() {
  const d = dialog.value
  dialogOut(d, () => d.close())
}

function onDialogClose() {
  open.value = false
  editingId.value = null
  Object.assign(form, empty())
  error.value = ''
  lockScroll(false)
}

async function save() {
  error.value = ''
  busy.value = true
  try {
    const body = { ...form, coffeeCount: Number(form.coffeeCount), price: Number(form.price), sortOrder: Number(form.sortOrder) }
    if (editingId.value) {
      await api(`/admin/packages/${editingId.value}`, { method: 'PUT', body })
      notify('Пакетът е обновен.')
    } else {
      await api('/admin/packages', { method: 'POST', body })
      notify('Пакетът е добавен.')
    }
    await load()
    closeEditor()
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="packages">
    <section v-spotlight v-reveal class="panel">
      <div class="panel-head">
        <div>
          <h2>Пакети</h2>
          <p class="hint">Натисни пакет, за да го промениш. Промяната не засяга вече направените покупки.</p>
        </div>
        <button type="button" class="btn btn-primary btn-block-phone" @click="openEditor()">Нов пакет</button>
      </div>
      <div class="table-wrap" data-lenis-prevent>
        <table class="ptable">
          <thead>
            <tr>
              <th>Име</th>
              <th class="num col-extra">Кафета</th>
              <th class="num">Цена</th>
              <th class="num col-extra">За кафе</th>
              <th class="col-extra">Видим</th>
              <th><span class="visually-hidden">Действия</span></th>
            </tr>
          </thead>
          <TransitionGroup tag="tbody" name="row-fade">
            <tr
              v-for="(p, i) in packages"
              :key="p.id"
              class="tap-row"
              :class="{ 'is-muted': !p.active }"
              :style="{ '--i': Math.min(i, 15) }"
              @click="openEditor(p)"
            >
              <td>
                {{ p.name }}
                <span class="row-meta">{{ money(p.price / p.coffeeCount) }} за кафе<template v-if="!p.active"> · скрит</template></span>
              </td>
              <td class="num col-extra">{{ p.coffeeCount }}</td>
              <td class="num">{{ money(p.price) }}</td>
              <td class="num col-extra">{{ money(p.price / p.coffeeCount) }}</td>
              <td class="col-extra">
                <span v-if="p.active" class="badge">да</span>
                <span v-else class="badge badge-off">скрит</span>
              </td>
              <td class="num">
                <button class="btn btn-small edit-btn" :aria-label="`Промени ${p.name}`" @click.stop="openEditor(p)">
                  <span class="hide-phone">Промени</span>
                  <span class="only-phone" aria-hidden="true">›</span>
                </button>
              </td>
            </tr>
            <tr v-if="!loading && !packages.length" key="empty">
              <td colspan="6" class="hint">Няма пакети. Добави първия с „Нов пакет“.</td>
            </tr>
          </TransitionGroup>
        </table>
      </div>
    </section>

    <dialog ref="dialog" autofocus @close="onDialogClose">
      <form v-if="open" class="dialog-body" @submit.prevent="save">
        <h2>{{ editingId ? 'Промени пакет' : 'Нов пакет' }}</h2>
        <div class="form-grid">
          <div class="field">
            <label for="pk-name">Име</label>
            <input id="pk-name" v-model="form.name" required maxlength="100" placeholder="5 кафета" />
          </div>
          <div class="field">
            <label for="pk-count">Брой кафета</label>
            <input id="pk-count" v-model.number="form.coffeeCount" type="number" inputmode="numeric" min="1" max="1000" required />
          </div>
          <div class="field">
            <label for="pk-price">Цена в евро</label>
            <input id="pk-price" v-model.number="form.price" type="number" inputmode="decimal" min="0" step="0.01" required />
          </div>
          <div class="field">
            <label for="pk-order">Позиция в списъка</label>
            <input id="pk-order" v-model.number="form.sortOrder" type="number" inputmode="numeric" />
          </div>
        </div>
        <div class="form-foot">
          <label class="check">
            <input v-model="form.active" type="checkbox" />
            Показвай пакета на колегите
          </label>
          <span v-if="perCoffee" class="hint">{{ perCoffee }} за кафе</span>
        </div>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <div class="dialog-actions">
          <button type="button" class="btn btn-quiet" @click="closeEditor">Откажи</button>
          <button class="btn btn-primary" :disabled="busy">{{ editingId ? 'Запази пакета' : 'Добави пакета' }}</button>
        </div>
      </form>
    </dialog>
  </div>
</template>

<style scoped>
.row-fade-enter-active { transition: opacity 0.35s ease, transform 0.35s ease; transition-delay: calc(var(--i, 0) * 40ms); }
.row-fade-leave-active { transition: opacity 0.25s ease; }
.row-fade-move { transition: transform 0.45s cubic-bezier(0.3, 1.1, 0.4, 1); }
.row-fade-enter-from { opacity: 0; transform: translateY(8px); }
.row-fade-leave-to { opacity: 0; }

.packages { display: grid; grid-template-columns: minmax(0, 1fr); gap: 1.5rem; }
.panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; flex-wrap: wrap; margin-bottom: 0.75rem; }
.panel-head .hint { margin-top: 0.25rem; }
.form-grid { grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); }
.form-foot { display: flex; gap: 0.5rem 1.5rem; align-items: center; flex-wrap: wrap; }
.tap-row { cursor: pointer; }
.tap-row:active td { background: var(--board); }

@media (max-width: 640px) {
  /* името на цял ред, числата по две */
  .form-grid { grid-template-columns: 1fr 1fr; }
  .form-grid .field:first-child { grid-column: 1 / -1; }
  .edit-btn { min-width: 44px; min-height: 44px; padding: 0; font-size: 1.5rem; font-weight: 500; border-color: transparent; color: var(--muted); }
}

.visually-hidden {
  position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap;
}
</style>
