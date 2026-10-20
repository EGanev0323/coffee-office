<script setup>
import { ref } from 'vue'
import { useSlidingIndicator } from '../../motion/useSlidingIndicator'
import { gsap, reducedMotion, ScrollTrigger, afterTick } from '../../motion'

const nav = ref(null)
const line = ref(null)
useSlidingIndicator(nav, line)

/* Подсекциите се сменят с плъзгане настрани – чертата под менюто води окото. */
function onEnter(el, done) {
  if (reducedMotion()) return done()
  gsap.fromTo(el, { x: 24, autoAlpha: 0 }, {
    x: 0,
    autoAlpha: 1,
    duration: 0.45,
    clearProps: 'transform,opacity,visibility',
    onComplete: () => { ScrollTrigger.refresh(); done() }
  })
}
function onLeave(el, done) {
  if (reducedMotion()) return afterTick(done)
  gsap.to(el, { x: -24, autoAlpha: 0, duration: 0.2, ease: 'power2.in', onComplete: done })
}
</script>

<template>
  <div class="admin">
    <nav ref="nav" class="subnav" aria-label="Админ секции" data-lenis-prevent>
      <RouterLink :to="{ name: 'admin' }" exact-active-class="is-active" active-class="">Месечен отчет</RouterLink>
      <RouterLink :to="{ name: 'admin-users' }">Колеги</RouterLink>
      <RouterLink :to="{ name: 'admin-packages' }">Пакети</RouterLink>
      <RouterLink :to="{ name: 'admin-qr' }">QR код</RouterLink>
      <span ref="line" class="subnav-line" aria-hidden="true"></span>
    </nav>
    <RouterView v-slot="{ Component, route }">
      <Transition :css="false" mode="out-in" @enter="onEnter" @leave="onLeave">
        <component :is="Component" :key="route.path" />
      </Transition>
    </RouterView>
  </div>
</template>

<style scoped>
.admin { display: grid; grid-template-columns: minmax(0, 1fr); gap: 1.5rem; }
.subnav {
  position: relative;
  display: flex;
  gap: 1.5rem;
  border-bottom: 1px solid var(--rule);
  /* на тесен екран секциите се плъзгат настрани, вместо да се пренасят */
  overflow-x: auto;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}
.subnav::-webkit-scrollbar { display: none; }
.subnav a {
  color: var(--muted);
  text-decoration: none;
  font-weight: 600;
  font-size: var(--step--1);
  padding: 0.75rem 0;
  margin-bottom: -1px;
  white-space: nowrap;
  flex: none;
  transition: color 0.2s ease;
}
.subnav a:hover { color: var(--ink); }
.subnav a.is-active { color: var(--ink); }
/* черта с маркер, която се плъзга под активната секция */
.subnav-line {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
  opacity: 0;
  border-bottom: 3px solid var(--marker);
  border-radius: 0 0 2px 2px;
  margin-bottom: -1px;
}
</style>
