<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'
import { coffees } from '../format'
import CoffeePoster from '../components/CoffeePoster.vue'
import { posterIntro } from '../motion/poster'
import SteamCup from '../components/fx/SteamCup.vue'
import { gsap, reducedMotion, afterTick } from '../motion'

/*
  Отваря се от QR кода на кафемашината. Отбелязва кафето веднага при отваряне,
  след което за кратко показва голям бутон „Отмени“.
  Самото отбелязване е POST заявка от страницата – простото зареждане на адреса
  (напр. преглед на линк в чат) не маха кафе.
*/
const UNDO_SECONDS = 15
const DOUBLE_SCAN_MS = 60 * 1000
const FRESH_SCAN_MS = 10 * 60 * 1000 // време от сканирането (вкл. вход), в което кафето се отбелязва само

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const state = ref('working') // working | confirm | done | undone | already | error
const error = ref('')
const todayCount = ref(1) // кое кафе за днес – избира надписа на плаката
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

onMounted(async () => {
  const at = Number(route.query.at)
  const fresh = !route.query.done && at > 0 && Date.now() - at < FRESH_SCAN_MS
  // Линкът важи веднъж: „Назад“ или стар таб, зареден наново (напр. след затваряне на браузъра),
  // показват бутон, а не отбелязват кафе сами – каквото и да е станало първия път.
  router.replace({ query: { done: '1' } })
  if (!fresh) {
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

/* Смяна на състоянията: старото избледнява, новото изплува; снимката „пада“ като в DrinkOverlay. */
function stageEnter(el, done) {
  if (reducedMotion()) return done()
  const polaroid = el.querySelector('.polaroid')
  if (!polaroid) {
    gsap.fromTo(el, { y: 20, autoAlpha: 0, scale: 0.97 }, { y: 0, autoAlpha: 1, scale: 1, duration: 0.5, ease: 'back.out(1.5)', clearProps: 'all', onComplete: done })
    return
  }
  gsap
    .timeline({ onComplete: done })
    .add(posterIntro(polaroid, { warning: todayCount.value >= 3 }), 0)
    .from(el.querySelectorAll('.remaining, .undo, .to-board'), { y: 16, autoAlpha: 0, stagger: 0.08, duration: 0.5 }, 0.6)
}
function stageLeave(el, done) {
  if (reducedMotion()) return afterTick(done)
  gsap.to(el, { y: -14, autoAlpha: 0, duration: 0.2, ease: 'power2.in', onComplete: done })
}

async function drink() {
  state.value = 'working'
  try {
    auth.user = await api('/me/consumptions', { method: 'POST' })
    const activity = await api('/me/activity')
    todayCount.value = drinksToday(activity)
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
    <Transition :css="false" mode="out-in" @enter="stageEnter" @leave="stageLeave">
    <div v-if="state === 'working'" key="working" class="working-stage" role="status">
      <SteamCup :size="120" />
      <p class="working hand">Отбелязвам…</p>
    </div>

    <section v-else-if="state === 'confirm'" key="confirm" class="panel message">
      <h1>Току-що отбеляза кафе</h1>
      <p class="hint">Преди {{ lastAgo }} сек. Наистина ли е още едно?</p>
      <div class="actions">
        <button class="btn btn-primary btn-big" @click="drink">Да, още едно</button>
        <RouterLink to="/" class="btn btn-big">Не</RouterLink>
      </div>
    </section>

    <div v-else-if="state === 'done'" key="done" class="done-stage">
      <div v-tilt="{ max: 8 }" class="polaroid">
        <span class="polaroid-pin" aria-hidden="true"></span>
        <CoffeePoster :count="todayCount" />
      </div>
      <p class="remaining" role="status">{{ remaining }}</p>

      <div v-if="secondsLeft > 0" class="undo">
        <button class="btn btn-big undo-btn" :disabled="busy" @click="undo">
          Отмени ({{ secondsLeft }})
        </button>
        <div class="undo-track" aria-hidden="true">
          <div class="undo-bar" :style="{ width: `${(secondsLeft / UNDO_SECONDS) * 100}%` }"></div>
        </div>
      </div>
      <RouterLink to="/" class="btn btn-quiet to-board">Към дъската</RouterLink>
    </div>

    <section v-else-if="state === 'undone'" key="undone" class="panel message">
      <h1>Отменено</h1>
      <p class="hint">Кафето е върнато на дъската. {{ remaining }}</p>
      <RouterLink to="/" class="btn btn-primary btn-big">Към дъската</RouterLink>
    </section>

    <section v-else-if="state === 'already'" key="already" class="panel message">
      <h1>Нищо не е отбелязано</h1>
      <p class="hint">Този линк от QR кода вече е бил отворен. Ако пиеш кафе сега, натисни бутона.</p>
      <div class="actions">
        <button class="btn btn-primary btn-big" @click="drink">Изпих кафе</button>
        <RouterLink to="/" class="btn btn-big">Към дъската</RouterLink>
      </div>
    </section>

    <section v-else-if="state === 'error'" key="error" class="panel message">
      <h1>Кафето не е отбелязано</h1>
      <p class="form-error" role="alert">{{ error }}</p>
      <RouterLink to="/" class="btn btn-primary btn-big">Към дъската</RouterLink>
    </section>
    </Transition>
  </div>
</template>

<style scoped>
.drink-page {
  min-height: calc(100vh - 7rem);
  min-height: calc(100dvh - var(--tabbar-h) - 7rem - var(--safe-bottom));
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 1.25rem;
  text-align: center;
  padding: 1rem 0 2rem;
}
.working { font-size: 2.6rem; }
.working-stage, .done-stage { display: grid; justify-items: center; gap: 1.25rem; }
.working-stage { gap: 0.5rem; }

.polaroid {
  position: relative;
  width: min(72vw, 300px, calc((100dvh - var(--tabbar-h) - 16rem) * 0.634));
  min-width: 180px;
  padding: 6px;
  background: #F4ECDD;
  border-radius: 4px;
  box-shadow: 0 18px 50px rgba(43, 31, 24, 0.25);
  transform: rotate(-2deg);
}
.polaroid-pin {
  position: absolute;
  z-index: 2;
  top: -10px;
  left: 50%;
  width: 24px;
  height: 24px;
  margin-left: -12px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #E46A5C, var(--red) 60%, #8A251B);
  box-shadow: 0 3px 6px rgba(43, 31, 24, 0.35);
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