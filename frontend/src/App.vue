<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'
import ConfirmDialog from './components/ConfirmDialog.vue'
import ToastStack from './components/ToastStack.vue'
import ClickSpark from './components/fx/ClickSpark.vue'
import BadgeUnlock from './components/BadgeUnlock.vue'
import AppIcon from './components/AppIcon.vue'
import markUrl from './assets/mark.svg'
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
      <RouterLink to="/" class="brand"><img :src="markUrl" alt="" width="34" height="34" />Кафе дъската</RouterLink>
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
      <AppIcon name="cup" />
      <span>Кафета</span>
    </RouterLink>
    <RouterLink v-if="auth.isAdmin" to="/admin" class="tab">
      <AppIcon name="admin" />
      <span>Админ</span>
    </RouterLink>
    <RouterLink to="/account" class="tab">
      <AppIcon name="user" />
      <span>Профил</span>
    </RouterLink>
  </nav>

  <ConfirmDialog />
  <ToastStack :raised="showNav && !typing" />
  <ClickSpark />
  <BadgeUnlock v-if="showNav" />
</template>
