<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import TallyMarks from '../components/TallyMarks.vue'
import SplitText from '../components/fx/SplitText.vue'
import SteamCup from '../components/fx/SteamCup.vue'
import FloatingBeans from '../components/fx/FloatingBeans.vue'
import { onMounted } from 'vue'
import { gsap, reducedMotion, shake } from '../motion'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const remember = ref(true)
const error = ref(route.query.expired ? 'Сесията изтече. Влез отново.' : '')
const busy = ref(false)
const formEl = ref(null)
const boardEl = ref(null)

onMounted(() => {
  if (reducedMotion()) return
  gsap.timeline({ defaults: { ease: 'power3.out' } })
    .from(formEl.value, { x: 60, autoAlpha: 0, rotation: 2, duration: 0.9 }, 0.15)
    .from(formEl.value.children, { y: 14, autoAlpha: 0, stagger: 0.06, duration: 0.5 }, 0.4)
    .from(boardEl.value.querySelector('.hint'), { y: 12, autoAlpha: 0, duration: 0.6 }, 0.9)
})

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
    shake(formEl.value)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="login">
    <FloatingBeans />
    <div ref="boardEl" class="login-board">
      <SteamCup class="login-cup" :size="104" :delay="0.1" />
      <SplitText
        tag="p"
        class="login-title hand"
        text="Кафе дъската"
        :delay="0.35"
        :stagger="0.05"
        :duration="0.8"
        :from="{ yPercent: 110, autoAlpha: 0, rotation: () => gsap.utils.random(-30, 30), scale: 0.4 }"
      />
      <TallyMarks :count="13" />
      <p class="hint">Купи кафета предварително и отбелязвай всяко изпито с едно натискане.</p>
    </div>

    <form ref="formEl" v-spotlight class="panel login-form" @submit.prevent="submit">
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
      <button v-magnet="{ strength: 0.15, padding: 20 }" class="btn btn-primary btn-login" type="submit" :disabled="busy">
        {{ busy ? 'Влизане…' : 'Влез' }}
      </button>
      <p class="hint">Нямаш профил? Администраторът на кафето ще ти създаде.</p>
    </form>
  </div>
</template>

<style scoped>
.login {
  position: relative;
  min-height: calc(100vh - 4rem);
  min-height: calc(100dvh - 4rem);
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 3rem;
  align-items: center;
  max-width: 900px;
  margin: 0 auto;
  padding-top: 2rem;
}
.login-board { position: relative; z-index: 1; display: grid; gap: 1.25rem; max-width: 30ch; }
.login-cup { margin-bottom: -0.5rem; }
.login-title { font-size: clamp(3rem, 8vw, 4.5rem); color: var(--ink); }
.login-form {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 1rem;
  box-shadow: 0 30px 60px -30px rgba(43, 31, 24, 0.28);
}
.btn-login { padding-block: 0.8rem; font-size: var(--step-0); }
@media (max-width: 720px) {
  .login { grid-template-columns: 1fr; gap: 2rem; min-height: auto; }
}
/* телефон: компактна глава (чаша до името), за да е формата в първия екран и над клавиатурата */
@media (max-width: 640px) {
  .login { gap: 1.25rem; padding-top: calc(var(--safe-top) + 1rem); }
  .login-board { grid-template-columns: auto 1fr; align-items: center; gap: 0.75rem; max-width: none; }
  .login-cup { width: 64px; height: 64px; margin-bottom: 0; }
  .login-title { font-size: 2.75rem; }
  .login-board :deep(.tally), .login-board > .hint { display: none; }
}
</style>
