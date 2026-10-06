<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../../api'
import { notify } from '../../notify'
import { useAuthStore } from '../../stores/auth'
import { coffees, money } from '../../format'

const auth = useAuthStore()
const users = ref([])
const packages = ref([])
const loading = ref(true)

const form = reactive({ username: '', displayName: '', password: '', role: 'USER' })
const createError = ref('')
const creating = ref(false)

const dialog = ref(null)
const editing = ref(null)
const editError = ref('')
const newPassword = ref('')
const purchasePackageId = ref(null)
const busy = ref(false)

const activePackages = computed(() => packages.value.filter((p) => p.active))
const isSelf = computed(() => editing.value?.id === auth.user?.id)

async function load() {
  try {
    const [u, p] = await Promise.all([api('/admin/users'), api('/admin/packages')])
    users.value = u
    packages.value = p
  } catch (e) {
    notify(e.message, 'error')
  } finally {
    loading.value = false
  }
}

async function create() {
  createError.value = ''
  creating.value = true
  try {
    const u = await api('/admin/users', { method: 'POST', body: { ...form } })
    notify(`Профилът на ${u.displayName} е създаден. Дай му потребителско име „${u.username}“ и паролата.`)
    Object.assign(form, { username: '', displayName: '', password: '', role: 'USER' })
    await load()
  } catch (e) {
    createError.value = e.message
  } finally {
    creating.value = false
  }
}

function openEditor(u) {
  editing.value = { ...u }
  editError.value = ''
  newPassword.value = ''
  purchasePackageId.value = activePackages.value[0]?.id ?? null
  dialog.value.showModal()
}
function closeEditor() {
  dialog.value.close()
  editing.value = null
}

async function withBusy(fn) {
  editError.value = ''
  busy.value = true
  try {
    await fn()
  } catch (e) {
    editError.value = e.message
  } finally {
    busy.value = false
  }
}

function saveUser() {
  return withBusy(async () => {
    const e = editing.value
    await api(`/admin/users/${e.id}`, {
      method: 'PUT',
      body: {
        displayName: e.displayName,
        role: e.role,
        active: e.active,
        unlimited: e.unlimited,
        boardVisible: e.boardVisible
      }
    })
    notify('Промените са запазени.')
    if (isSelf.value) await auth.fetchMe()
    await load()
    closeEditor()
  })
}

function resetPassword() {
  return withBusy(async () => {
    await api(`/admin/users/${editing.value.id}/reset-password`, {
      method: 'POST',
      body: { password: newPassword.value }
    })
    newPassword.value = ''
    notify(`Паролата на ${editing.value.displayName} е сменена.`)
  })
}

function recordPurchase() {
  return withBusy(async () => {
    const pkg = packages.value.find((p) => p.id === purchasePackageId.value)
    const res = await api(`/admin/users/${editing.value.id}/purchases`, {
      method: 'POST',
      body: { packageId: purchasePackageId.value }
    })
    editing.value.balance = res.balance
    if (isSelf.value) auth.user = res
    notify(`${editing.value.displayName}: добавени ${coffees(pkg.coffeeCount)} за ${money(pkg.price)}.`)
    await load()
  })
}

onMounted(load)
</script>

<template>
  <div class="users">
    <form class="panel" @submit.prevent="create">
      <h2>Нов колега</h2>
      <p class="hint">Колегата влиза с потребителското име и паролата, после може да си смени паролата от „Профил“.</p>
      <div class="form-grid create-grid">
        <div class="field">
          <label for="nu-name">Име</label>
          <input id="nu-name" v-model="form.displayName" required maxlength="100" placeholder="Мария Петрова" />
        </div>
        <div class="field">
          <label for="nu-user">Потребителско име</label>
          <input id="nu-user" v-model="form.username" required minlength="3" maxlength="50"
                 pattern="[A-Za-z0-9._\-]+" autocapitalize="none" placeholder="maria" />
        </div>
        <div class="field">
          <label for="nu-pass">Начална парола</label>
          <input id="nu-pass" v-model="form.password" type="text" required minlength="6" autocomplete="off" />
        </div>
        <div class="field">
          <label for="nu-role">Роля</label>
          <select id="nu-role" v-model="form.role">
            <option value="USER">Колега</option>
            <option value="ADMIN">Администратор</option>
          </select>
        </div>
        <button class="btn btn-primary" :disabled="creating">Създай профил</button>
      </div>
      <p v-if="createError" class="form-error" role="alert">{{ createError }}</p>
    </form>

    <section class="panel">
      <h2>Колеги</h2>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Име</th>
              <th>Потребител</th>
              <th>Роля</th>
              <th class="num">Налични</th>
              <th><span class="visually-hidden">Действия</span></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id" :class="{ 'is-muted': !u.active }">
              <td>
                {{ u.displayName }}
                <span v-if="!u.active" class="badge badge-off">неактивен</span>
                <span v-else-if="!u.boardVisible" class="badge">скрит от дъската</span>
              </td>
              <td>{{ u.username }}</td>
              <td>{{ u.role === 'ADMIN' ? 'Администратор' : 'Колега' }}</td>
              <td class="num">{{ u.unlimited ? '∞+1' : u.balance }}</td>
              <td class="num"><button class="btn btn-small" @click="openEditor(u)">Управлявай</button></td>
            </tr>
            <tr v-if="!loading && !users.length">
              <td colspan="5" class="hint">Все още няма колеги.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <dialog ref="dialog" @close="editing = null">
      <div v-if="editing" class="dialog-body">
        <div class="dialog-head">
          <h2>{{ editing.displayName }}</h2>
          <p class="hint">
            {{ editing.username }},
            {{ editing.unlimited ? 'безкрайни кафета' : `${coffees(editing.balance)} на дъската` }}
          </p>
        </div>

        <form class="editor-section" @submit.prevent="saveUser">
          <div class="field">
            <label for="ed-name">Име</label>
            <input id="ed-name" v-model="editing.displayName" required maxlength="100" />
          </div>
          <div class="field">
            <label for="ed-role">Роля</label>
            <select id="ed-role" v-model="editing.role" :disabled="isSelf">
              <option value="USER">Колега</option>
              <option value="ADMIN">Администратор</option>
            </select>
          </div>
          <label class="check">
            <input v-model="editing.active" type="checkbox" :disabled="isSelf" />
            Активен профил (неактивните не могат да влизат)
          </label>
          <label class="check">
            <input v-model="editing.boardVisible" type="checkbox" />
            Показвай на дъската
          </label>
          <label class="check">
            <input v-model="editing.unlimited" type="checkbox" />
            Безкрайни кафета (балансът не намалява, не купува пакети)
          </label>
          <button class="btn btn-primary" :disabled="busy">Запази промените</button>
        </form>

        <form v-if="!editing.unlimited" class="editor-section" @submit.prevent="recordPurchase">
          <h3>Запиши плащане</h3>
          <p class="hint">Когато колегата е дал парите директно на теб.</p>
          <div class="inline">
            <select v-model="purchasePackageId" class="inline-select" aria-label="Пакет" required>
              <option v-for="p in activePackages" :key="p.id" :value="p.id">
                {{ p.name }} за {{ money(p.price) }}
              </option>
            </select>
            <button class="btn" :disabled="busy || !purchasePackageId">Добави кафетата</button>
          </div>
        </form>

        <p v-if="isSelf" class="hint editor-section">Собствената си парола смени от „Профил“.</p>
        <form v-else class="editor-section" @submit.prevent="resetPassword">
          <h3>Нова парола</h3>
          <p class="hint">Колегата ще бъде отписан от всичките си устройства.</p>
          <div class="inline">
            <input v-model="newPassword" class="inline-input" type="text" minlength="6" required
                   autocomplete="off" aria-label="Нова парола" placeholder="поне 6 символа" />
            <button class="btn" :disabled="busy">Смени паролата</button>
          </div>
        </form>

        <p v-if="editError" class="form-error" role="alert">{{ editError }}</p>
        <div class="dialog-actions">
          <button type="button" class="btn btn-quiet" @click="closeEditor">Затвори</button>
        </div>
      </div>
    </dialog>
  </div>
</template>

<style scoped>
.users { display: grid; grid-template-columns: minmax(0, 1fr); gap: 1.5rem; }
.panel h2 + .hint { margin: 0.25rem 0 1rem; }
.create-grid { grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); }
dialog { width: min(94vw, 520px); }
.dialog-head { display: grid; gap: 0.2rem; }
.editor-section { display: grid; gap: 0.75rem; padding-top: 1rem; border-top: 1px solid var(--rule); }
.editor-section h3 { font-size: var(--step-0); }
.editor-section .btn-primary { justify-self: start; }
.inline { display: flex; gap: 0.5rem; flex-wrap: wrap; }
.inline-select, .inline-input {
  flex: 1 1 200px;
  font: inherit;
  padding: 0.55rem 0.75rem;
  border: 1px solid var(--rule);
  border-radius: 8px;
  background: var(--surface);
  color: var(--ink);
}
.visually-hidden {
  position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap;
}
</style>
