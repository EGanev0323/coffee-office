<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../../api'
import { notify } from '../../notify'
import { money } from '../../format'

const packages = ref([])
const loading = ref(true)
const editingId = ref(null)
const error = ref('')
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

function edit(p) {
  editingId.value = p.id
  Object.assign(form, { name: p.name, coffeeCount: p.coffeeCount, price: p.price, active: p.active, sortOrder: p.sortOrder })
  error.value = ''
}
function cancel() {
  editingId.value = null
  Object.assign(form, empty())
  error.value = ''
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
    cancel()
    await load()
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
    <form class="panel" @submit.prevent="save">
      <h2>{{ editingId ? 'Промени пакет' : 'Нов пакет' }}</h2>
      <p class="hint">Промяната на пакет не засяга вече направените покупки.</p>
      <div class="form-grid">
        <div class="field">
          <label for="pk-name">Име</label>
          <input id="pk-name" v-model="form.name" required maxlength="100" placeholder="5 кафета" />
        </div>
        <div class="field">
          <label for="pk-count">Брой кафета</label>
          <input id="pk-count" v-model.number="form.coffeeCount" type="number" min="1" max="1000" required />
        </div>
        <div class="field">
          <label for="pk-price">Цена в евро</label>
          <input id="pk-price" v-model.number="form.price" type="number" min="0" step="0.01" required />
        </div>
        <div class="field">
          <label for="pk-order">Позиция в списъка</label>
          <input id="pk-order" v-model.number="form.sortOrder" type="number" />
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
      <div class="form-actions">
        <button class="btn btn-primary" :disabled="busy">{{ editingId ? 'Запази пакета' : 'Добави пакета' }}</button>
        <button v-if="editingId" type="button" class="btn btn-quiet" @click="cancel">Откажи</button>
      </div>
    </form>

    <section class="panel">
      <h2>Пакети</h2>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Име</th>
              <th class="num">Кафета</th>
              <th class="num">Цена</th>
              <th class="num">За кафе</th>
              <th>Видим</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in packages" :key="p.id" :class="{ 'is-muted': !p.active }">
              <td>{{ p.name }}</td>
              <td class="num">{{ p.coffeeCount }}</td>
              <td class="num">{{ money(p.price) }}</td>
              <td class="num">{{ money(p.price / p.coffeeCount) }}</td>
              <td>
                <span v-if="p.active" class="badge">да</span>
                <span v-else class="badge badge-off">скрит</span>
              </td>
              <td class="num"><button class="btn btn-small" @click="edit(p)">Промени</button></td>
            </tr>
            <tr v-if="!loading && !packages.length">
              <td colspan="6" class="hint">Няма пакети. Добави първия отгоре.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.packages { display: grid; gap: 1.5rem; }
.panel h2 + .hint { margin: 0.25rem 0 1rem; }
.form-foot { display: flex; gap: 1.5rem; align-items: center; flex-wrap: wrap; margin-top: 1rem; }
.form-actions { display: flex; gap: 0.5rem; margin-top: 1rem; }
</style>
