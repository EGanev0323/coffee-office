<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'
import ConfirmDialog from './components/ConfirmDialog.vue'
import ToastStack from './components/ToastStack.vue'
import ClickSpark from './components/fx/ClickSpark.vue'
import { gsap, initSmoothScroll, reducedMotion, ScrollTrigger } from './motion'
import { pageEnter, pageLeave } from './motion/pageTransition'
import { useSlidingIndicator } from './motion/useSlidingIndicator'

/*
  Рамката на приложението за телефон: горе само името, долу лента с табове в зоната на палеца.
  „Изход“ е в „Профил“, за да не се натиска случайно.
*/
const auth = useAuthStore()
const route = useRoute()

const showNav = computed(() => auth.isLoggedIn && auth.user && !route.meta.public)

const header = ref(null)
const tabs = ref(null)
const pill = ref(null)
useSlidingIndicator(tabs, pill)

// Ключ за прехода – само горното ниво, за да не се преиграва цялата админ рамка при смяна на подсекция.
const pageKey = computed(() => route.matched[0]?.path ?? route.path)

// Докато е отворена клавиатурата, лентата с табове се скрива, за да не покрива полето.
const typing = ref(false)
const FIELDS = 'input:not([type="checkbox"]):not([type="radio"]), textarea, select'
function onFocusIn(e) {
  if (e.target instanceof Element && e.target.matches(FIELDS)) typing.value = true
}
function onFocusOut() {
  requestAnimationFrame(() => {
    typing.value = !!document.activeElement?.matches?.(FIELDS)
  })
}

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
watch(tabs, (el) => {
  if (el && !reducedMotion()) gsap.from(el, { yPercent: 100, duration: 0.6, ease: 'power3.out', delay: 0.1, clearProps: 'transform' })
})

onMounted(() => {
  initSmoothScroll()
  document.addEventListener('focusin', onFocusIn)
  document.addEventListener('focusout', onFocusOut)
})
onBeforeUnmount(() => {
  hideTrigger?.kill()
  document.removeEventListener('focusin', onFocusIn)
  document.removeEventListener('focusout', onFocusOut)
})
</script>

<template>
  <header v-if="showNav" ref="header" class="topbar-wrap">
    <div class="topbar">
      <RouterLink to="/" class="brand">Кафе дъската</RouterLink>
    </div>
  </header>

  <main class="page" :class="{ 'has-tabs': showNav }">
    <RouterView v-slot="{ Component }">
      <Transition :css="false" mode="out-in" @enter="pageEnter" @leave="pageLeave">
        <component :is="Component" :key="pageKey" />
      </Transition>
    </RouterView>
  </main>

  <nav v-if="showNav" ref="tabs" class="tabbar" :class="{ 'is-hidden': typing }" aria-label="Основна навигация">
    <span ref="pill" class="tabbar-pill" aria-hidden="true"></span>
    <RouterLink to="/" class="tab" exact-active-class="is-active" active-class="">
      <svg class="tab-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4.5 9h12v5.5a5 5 0 0 1-5 5h-2a5 5 0 0 1-5-5z" />
        <path d="M16.5 10.5h1.2a2.6 2.6 0 0 1 0 5.2h-1.5" />
        <path d="M8.5 3.5c-.8 1 .8 1.8 0 3M12.5 3.5c-.8 1 .8 1.8 0 3" />
      </svg>
      <span>Кафета</span>
    </RouterLink>
    <RouterLink v-if="auth.isAdmin" to="/admin" class="tab">
      <svg class="tab-icon" viewBox="0 0 24 24" aria-hidden="true">
        <rect x="4.5" y="4" width="15" height="16.5" rx="2" />
        <path d="M8.5 15.5v-3M12 15.5v-6M15.5 15.5v-4.5" />
      </svg>
      <span>Админ</span>
    </RouterLink>
    <RouterLink to="/account" class="tab">
      <svg class="tab-icon" viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="8.5" r="3.8" />
        <path d="M4.5 20c.9-3.6 3.9-5.5 7.5-5.5s6.6 1.9 7.5 5.5" />
      </svg>
      <span>Профил</span>
    </RouterLink>
  </nav>

  <ConfirmDialog />
  <ToastStack :raised="showNav && !typing" />
  <ClickSpark />
</template>
