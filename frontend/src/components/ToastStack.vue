<script setup>
import { toasts } from '../notify'
import { gsap, reducedMotion, afterTick } from '../motion'

/* Съобщенията изскачат отдолу с лек подскок и отлитат встрани, когато изтекат. */
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
  <div class="toasts" role="status" aria-live="polite">
    <TransitionGroup :css="false" @enter="onEnter" @leave="onLeave">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast"
        :class="{ 'toast-error': t.type === 'error' }"
        :data-ttl="t.type === 'error' ? 6000 : 3500"
      >
        {{ t.message }}
        <span class="toast-timer" aria-hidden="true"></span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast { position: relative; overflow: hidden; box-shadow: 0 12px 30px -10px rgba(43, 31, 24, 0.45); }
.toast-timer {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  background: rgba(255, 255, 255, 0.45);
  transform-origin: left center;
}
</style>
