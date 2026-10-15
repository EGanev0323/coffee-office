<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import ConfirmDialog from './components/ConfirmDialog.vue'
import ToastStack from './components/ToastStack.vue'
import ClickSpark from './components/fx/ClickSpark.vue'
import { gsap, initSmoothScroll, reducedMotion, ScrollTrigger } from './motion'
import { pageEnter, pageLeave } from './motion/pageTransition'
import { useSlidingIndicator } from './motion/useSlidingIndicator'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const showNav = computed(() => auth.isLoggedIn && auth.user && !route.meta.public)

const header = ref(null)
const nav = ref(null)
const pill = ref(null)
useSlidingIndicator(nav, pill)

// Ключ за прехода – само горното ниво, за да не се преиграва цялата админ рамка при смяна на подсекция.
const pageKey = computed(() => route.matched[0]?.path ?? route.path)

let hideTrigger = null

/** Горната лента се скрива при скролване надолу и се връща при скролване нагоре. */
function setupHeader() {
  hideTrigger?.kill()
  hideTrigger = null
  if (!header.value || reducedMotion()) return
  const el = header.value
  gsap.from(el, { yPercent: -100, autoAlpha: 0, duration: 0.6, ease: 'power3.out', clearProps: 'all' })
  let hidden = false
  hideTrigger = ScrollTrigger.create({
    start: 0,
    end: 'max',
    onUpdate: (self) => {
      el.classList.toggle('is-scrolled', self.scroll() > 8)
      const hide = self.direction === 1 && self.scroll() > 120
      if (hide !== hidden) {
        hidden = hide
        gsap.to(el, { yPercent: hide ? -110 : 0, duration: 0.4, ease: hide ? 'power2.in' : 'power3.out' })
      }
    }
  })
}

watch(header, (el) => { if (el) setupHeader() })

onMounted(() => {
  initSmoothScroll()
})
onBeforeUnmount(() => hideTrigger?.kill())

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header v-if="showNav" ref="header" class="topbar-wrap">
    <div class="topbar">
      <RouterLink to="/" class="brand">Кафе дъската</RouterLink>
      <nav ref="nav" class="mainnav" aria-label="Основна навигация">
        <span ref="pill" class="mainnav-pill" aria-hidden="true"></span>
        <RouterLink v-magnet="{ strength: 0.2, padding: 10 }" to="/" exact-active-class="is-active" active-class="">Моите кафета</RouterLink>
        <RouterLink v-if="auth.isAdmin" v-magnet="{ strength: 0.2, padding: 10 }" to="/admin">Админ</RouterLink>
        <RouterLink v-magnet="{ strength: 0.2, padding: 10 }" to="/account">Профил</RouterLink>
        <button v-magnet="{ strength: 0.2, padding: 10 }" type="button" class="linklike" @click="logout">Изход</button>
      </nav>
    </div>
  </header>
  <main class="page">
    <RouterView v-slot="{ Component }">
      <Transition :css="false" mode="out-in" @enter="pageEnter" @leave="pageLeave">
        <component :is="Component" :key="pageKey" />
      </Transition>
    </RouterView>
  </main>
  <ConfirmDialog />
  <ToastStack />
  <ClickSpark />
</template>
