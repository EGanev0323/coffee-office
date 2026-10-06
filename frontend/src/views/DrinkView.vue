<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'
import { coffees } from '../format'
import photo from '../assets/coffee-face.jpg'

/*
  Отваря се от QR кода на кафемашината. Отбелязва кафето веднага при отваряне,
  след което за кратко показва голям бутон „Отмени“.
  Самото отбелязване е POST заявка от страницата – простото зареждане на адреса
  (напр. преглед на линк в чат) не маха кафе.
*/
const UNDO_SECONDS = 15
const DOUBLE_SCAN_MS = 60 * 1000

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const state = ref('working') // working | confirm | done | undone | already | error
const error = ref('')
const caption = ref({ text: 'Ти изпи кафе', warning: false })
const secondsLeft = ref(UNDO_SECONDS)
const lastAgo = ref(0)
const busy = ref(false)
let timer

const remaining = computed(() => {
  if (!auth.user) return ''
  if (auth.user.unlimited) return 'Имаш безкрайни кафета.'
  return auth.user.balance === 0
    ? 'Това беше последното ти кафе. Купи пакет от дъската.'
    : `Остават ти ${coffees(auth.user.balance)}.`
})

function drinksToday(activity) {
  const today = new Date().toDateString()
  return activity.filter(
    (a) => a.type === 'CONSUMPTION' && new Date(a.createdAt).toDateString() === today
  ).length
}

function captionFor(count) {
  if (count <= 1) return { text: 'Ти изпи кафе', warning: false }
  if (count === 2) return { text: 'Ти изпи второ кафе', warning: false }
  return { text: 'Много кафета пиеш', warning: true }
}

onMounted(async () => {
  // Връщане назад в историята след вече отбелязано кафе – не отбелязваме пак.
  if (route.query.done) {
    state.value = 'already'
    return
  }
  try {
    const activity = await api('/me/activity')
    const last = activity.find((a) => a.type === 'CONSUMPTION')
    const ago = last ? Date.now() - new Date(last.createdAt).getTime() : Infinity
    if (ago < DOUBLE_SCAN_MS) {
      lastAgo.value = Math.max(1, Math.round(ago / 1000))
      state.value = 'confirm'
      return
    }
    await drink()
  } catch (e) {
    error.value = e.message
    state.value = 'error'
  }
})

onUnmounted(() => clearInterval(timer))

async function drink() {
  state.value = 'working'
  try {
    auth.user = await api('/me/consumptions', { method: 'POST' })
    const activity = await api('/me/activity')
    caption.value = captionFor(drinksToday(activity))
    state.value = 'done'
    router.replace({ query: { done: '1' } }) // „Назад“ после няма да отбележи второ кафе
    startCountdown()
  } catch (e) {
    error.value = e.message
    state.value = 'error'
  }
}

function startCountdown() {
  secondsLeft.value = UNDO_SECONDS
  clearInterval(timer)
  timer = setInterval(() => {
    secondsLeft.value -= 1
    if (secondsLeft.value <= 0) clearInterval(timer)
  }, 1000)
}

async function undo() {
  if (busy.value) return
  busy.value = true
  clearInterval(timer)
  try {
    auth.user = await api('/me/consumptions/last', { method: 'DELETE' })
    state.value = 'undone'
  } catch (e) {
    error.value = e.message
    state.value = 'error'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="drink-page">
    <p v-if="state === 'working'" class="working hand" role="status">Отбелязвам…</p>

    <section v-else-if="state === 'confirm'" class="panel message">
      <h1>Току-що отбеляза кафе</h1>
      <p class="hint">Преди {{ lastAgo }} сек. Наистина ли е още едно?</p>
      <div class="actions">
        <button class="btn btn-primary btn-big" @click="drink">Да, още едно</button>
        <RouterLink to="/" class="btn btn-big">Не</RouterLink>
      </div>
    </section>

    <template v-else-if="state === 'done'">
      <figure class="polaroid">
        <img :src="photo" alt="" width="280" height="280" />
        <figcaption :class="{ 'is-warning': caption.warning }">{{ caption.text }}</figcaption>
      </figure>
      <p class="remaining" role="status">{{ remaining }}</p>

      <div v-if="secondsLeft > 0" class="undo">
        <button class="btn btn-big undo-btn" :disabled="busy" @click="undo">
          Отмени ({{ secondsLeft }})
        </button>
        <div class="undo-track" aria-hidden="true">
          <div class="undo-bar" :style="{ width: `${(secondsLeft / UNDO_SECONDS) * 100}%` }"></div>
        </div>
      </div>
      <RouterLink to="/" class="btn btn-quiet">Към дъската</RouterLink>
    </template>

    <section v-else-if="state === 'undone'" class="panel message">
      <h1>Отменено</h1>
      <p class="hint">Кафето е върнато на дъската. {{ remaining }}</p>
      <RouterLink to="/" class="btn btn-primary btn-big">Към дъската</RouterLink>
    </section>

    <section v-else-if="state === 'already'" class="panel message">
      <h1>Това кафе вече е отбелязано</h1>
      <div class="actions">
        <button class="btn btn-primary btn-big" @click="drink">Изпих още едно</button>
        <RouterLink to="/" class="btn btn-big">Към дъската</RouterLink>
      </div>
    </section>

    <section v-else-if="state === 'error'" class="panel message">
      <h1>Кафето не е отбелязано</h1>
      <p class="form-error" role="alert">{{ error }}</p>
      <RouterLink to="/" class="btn btn-primary btn-big">Към дъската</RouterLink>
    </section>
  </div>
</template>

<style scoped>
.drink-page {
  min-height: calc(100vh - 7rem);
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 1.25rem;
  text-align: center;
  padding: 1rem 0 2rem;
}
.working { font-size: 2.6rem; }

.polaroid {
  margin: 0;
  background: #FFFFFF;
  padding: 14px 14px 10px;
  border-radius: 6px;
  box-shadow: 0 18px 50px rgba(43, 31, 24, 0.22);
  transform: rotate(-2deg);
  animation: pop 0.38s cubic-bezier(0.2, 1.4, 0.4, 1) both;
}
.polaroid img {
  display: block;
  width: min(280px, 72vw);
  height: auto;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 2px;
}
.polaroid figcaption {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 2.2rem;
  line-height: 1.1;
  color: var(--marker);
  margin-top: 0.5rem;
  max-width: min(280px, 72vw);
}
.polaroid figcaption.is-warning { color: var(--red); }
@keyframes pop {
  from { transform: scale(0.8) rotate(-8deg); opacity: 0; }
  to { transform: rotate(-2deg); opacity: 1; }
}

.remaining { font-weight: 500; }

.undo { display: grid; gap: 0.5rem; width: min(320px, 90vw); }
.undo-btn { width: 100%; border-color: var(--ink); }
.undo-track { height: 4px; background: var(--rule); border-radius: 2px; overflow: hidden; }
.undo-bar { height: 100%; background: var(--ink); transition: width 1s linear; }

.btn-big { font-size: var(--step-1); padding: 0.9rem 1.6rem; border-radius: 10px; text-decoration: none; }

.message { display: grid; gap: 1rem; justify-items: center; width: min(440px, 100%); }
.actions { display: flex; gap: 0.75rem; flex-wrap: wrap; justify-content: center; }
</style>