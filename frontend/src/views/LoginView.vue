<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import TallyMarks from '../components/TallyMarks.vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const remember = ref(true)
const error = ref(route.query.expired ? 'Сесията изтече. Влез отново.' : '')
const busy = ref(false)

/** Само вътрешен път – без „//друг-сайт“ и пълни адреси. */
function safeNext(next) {
  return typeof next === 'string' && next.startsWith('/') && !next.startsWith('//') ? next : null
}

async function submit() {
  error.value = ''
  busy.value = true
  try {
    await auth.login(username.value.trim(), password.value, remember.value)
    const next = safeNext(route.query.next)
    router.replace(next || (auth.isAdmin ? '/admin' : '/'))
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="login">
    <div class="login-board">
      <p class="login-title hand">Кафе дъската</p>
      <TallyMarks :count="13" />
      <p class="hint">Купи кафета предварително и отбелязвай всяко изпито с едно натискане.</p>
    </div>

    <form class="panel login-form" @submit.prevent="submit">
      <h1>Вход</h1>
      <div class="field">
        <label for="username">Потребителско име</label>
        <input id="username" v-model="username" autocomplete="username" autocapitalize="none" required />
      </div>
      <div class="field">
        <label for="password">Парола</label>
        <input id="password" v-model="password" type="password" autocomplete="current-password" required />
      </div>
      <label class="check">
        <input v-model="remember" type="checkbox" />
        Запомни ме на това устройство
      </label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button class="btn btn-primary" type="submit" :disabled="busy">
        {{ busy ? 'Влизане…' : 'Влез' }}
      </button>
      <p class="hint">Нямаш профил? Администраторът на кафето ще ти създаде.</p>
    </form>
  </div>
</template>

<style scoped>
.login {
  min-height: calc(100vh - 4rem);
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 3rem;
  align-items: center;
  max-width: 900px;
  margin: 0 auto;
  padding-top: 2rem;
}
.login-board { display: grid; gap: 1.25rem; max-width: 30ch; }
.login-title { font-size: clamp(3rem, 8vw, 4.5rem); color: var(--ink); }
.login-form { display: grid; gap: 1rem; }
@media (max-width: 720px) {
  .login { grid-template-columns: 1fr; gap: 2rem; min-height: auto; }
}
</style>
