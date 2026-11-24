<script setup>
import { toasts } from '../notify'
import { gsap, reducedMotion, afterTick } from '../motion'
import AppIcon from './AppIcon.vue'

defineProps({ raised: { type: Boolean, default: false } }) // над долната лента с табове

/* Съобщенията са малки бележки долу вдясно: залепват с лек подскок и отлитат встрани, когато изтекат. */
function onEnter(el, done) {
  if (reducedMotion()) return done()
  gsap.fromTo(
    el,
    { y: 28, scale: 0.85, autoAlpha: 0 },
    { y: 0, scale: 1, autoAlpha: 1, duration: 0.55, ease: 'back.out(1.8)', onComplete: done }
  )
  const bar = el.querySelector('.toast-timer')
  if (bar) gsap.fromTo(bar, { scaleX: 1 }, { scaleX: 0, duration: Number(el.dataset.ttl) / 1000, ease: 'none' })
}
function onLeave(el, done) {
  if (reducedMotion()) return afterTick(done)
  gsap.to(el, { x: 60, autoAlpha: 0, scale: 0.9, duration: 0.3, ease: 'power2.in', onComplete: done })
}
</script>

<template>
  <div class="toasts" :class="{ 'is-raised': raised }" role="status" aria-live="polite">
    <TransitionGroup :css="false" @enter="onEnter" @leave="onLeave">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast"
        :class="t.type === 'error' ? 'toast-error' : ''"
        :data-ttl="t.type === 'error' ? 6000 : 3500"
      >
        <AppIcon :name="t.type === 'error' ? 'bell' : 'check'" />
        <span class="toast-text">{{ t.message }}</span>
        <span class="toast-timer" aria-hidden="true"></span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toasts {
  position: fixed;
  right: var(--gutter-r);
  bottom: calc(1.25rem + var(--safe-bottom));
  z-index: 50;
  display: grid;
  gap: 0.5rem;
  justify-items: end;
  width: min(92vw, 22rem);
  transition: bottom 0.25s ease;
}
.toasts.is-raised { bottom: calc(var(--tabbar-h) + 0.75rem + var(--safe-bottom)); }
/* над плаващия бутон „Изпих кафе“ / лентата за рисуване (HomeView / WhiteBoard слагат has-fab на body) */
:global(body.has-fab) .toasts.is-raised { bottom: calc(var(--tabbar-h) + var(--fab-h) + 1.5rem + var(--safe-bottom)); }

.toast {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  max-width: 100%;
  padding: 0.6rem 0.9rem 0.7rem;
  background: var(--sticky-green);
  color: var(--sticky-ink);
  border-radius: var(--radius-sticky);
  box-shadow: var(--shadow-sticky);
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 1.375rem;
  line-height: 1.1;
}
.toast .ico { width: 1.125rem; height: 1.125rem; }
.toast-text { min-width: 0; }
.toast-error { background: var(--sticky-pink); }
.toast-timer {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  background: rgba(61, 52, 16, 0.3);
  transform-origin: left center;
}
</style>
