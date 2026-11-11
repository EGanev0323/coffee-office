<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { BADGES, unlocked } from '../badges'
import { afterTick, gsap, haptic, reducedMotion } from '../motion'
import { burst } from '../motion/burst'

/*
  „Нов бадж!“ – показва се над всичко, когато изпито кафе отключи бадж.
  Магнитчето се завърта и щраква, наоколо хвърчат зърна, а после се затваря само (или с докосване).
*/
const router = useRouter()
const show = ref(false)
const code = ref(null)
let showTimer
let hideTimer

const SHOW_MS = 5000

watch(() => unlocked.code, (c) => {
  if (!c) return
  clearTimeout(showTimer)
  showTimer = setTimeout(() => {
    code.value = c
    show.value = true
    unlocked.code = null
    haptic([20, 80, 20, 80, 40])
    clearTimeout(hideTimer)
    hideTimer = setTimeout(close, SHOW_MS)
  }, unlocked.delay)
})

function close() {
  clearTimeout(hideTimer)
  show.value = false
}

function openProfile() {
  close()
  router.push('/account')
}

function onKey(e) {
  if (show.value && e.key === 'Escape') close()
}
window.addEventListener('keydown', onKey)
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
  clearTimeout(showTimer)
  clearTimeout(hideTimer)
})

function onEnter(el, done) {
  if (reducedMotion()) return done()
  const medal = el.querySelector('.unlock-medal')
  const rays = el.querySelector('.unlock-rays')
  gsap
    .timeline({ onComplete: done })
    .fromTo(el, { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.25 })
    .fromTo(rays, { scale: 0.3, autoAlpha: 0 }, { scale: 1, autoAlpha: 1, duration: 0.8, ease: 'power2.out' }, 0.1)
    .fromTo(
      medal,
      { scale: 0.2, rotationY: -540, y: 60 },
      { scale: 1, rotationY: 0, y: 0, duration: 1.1, ease: 'back.out(1.6)', transformPerspective: 700 },
      0.05
    )
    .add(() => burst(medal, { kind: 'beans', count: 22, power: 1.3 }), 0.75)
    .to(medal, { scale: 1.08, duration: 0.12, yoyo: true, repeat: 1, ease: 'power2.out' }, 0.95) // „щрак“
    .from(el.querySelectorAll('.unlock-text > *'), { y: 16, autoAlpha: 0, stagger: 0.1, duration: 0.45 }, 0.6)
}

function onLeave(el, done) {
  if (reducedMotion()) return afterTick(done)
  gsap.to(el, { autoAlpha: 0, duration: 0.25, onComplete: done })
}
</script>

<template>
  <Teleport to="body">
    <Transition :css="false" @enter="onEnter" @leave="onLeave">
      <div v-if="show && code" class="unlock" role="status" aria-live="polite" @click="close">
        <div class="unlock-stage">
          <span class="unlock-rays" aria-hidden="true"></span>
          <img :src="BADGES[code].img" alt="" class="unlock-medal" width="240" height="240" draggable="false" />
        </div>
        <div class="unlock-text">
          <p class="unlock-kicker">Нов бадж!</p>
          <p class="unlock-title">{{ BADGES[code].title }}</p>
          <p class="unlock-hint">Магнитчето вече е на дъската до името ти.</p>
          <button type="button" class="btn unlock-btn" @click.stop="openProfile">Виж баджовете</button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.unlock {
  position: fixed;
  inset: 0;
  z-index: 65;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 1.25rem;
  padding: max(1rem, env(safe-area-inset-top, 0px)) 1.25rem max(1rem, env(safe-area-inset-bottom, 0px));
  background: radial-gradient(ellipse at 50% 40%, rgba(40, 30, 70, 0.92), rgba(12, 10, 20, 0.96));
  color: #fff;
  text-align: center;
  cursor: pointer;
  overflow: hidden;
}
.unlock-stage { position: relative; display: grid; place-items: center; }
/* въртящи се лъчи зад медала */
.unlock-rays {
  position: absolute;
  width: min(150vw, 640px);
  aspect-ratio: 1;
  border-radius: 50%;
  background: repeating-conic-gradient(rgba(255, 220, 140, 0.16) 0deg 8deg, transparent 8deg 20deg);
  -webkit-mask: radial-gradient(circle, #000 18%, transparent 68%);
  mask: radial-gradient(circle, #000 18%, transparent 68%);
  animation: unlock-spin 14s linear infinite;
}
@keyframes unlock-spin { to { rotate: 360deg; } }
.unlock-medal {
  position: relative;
  width: min(62vw, 240px);
  height: auto;
  aspect-ratio: 1;
  border-radius: 50%;
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.12), 0 24px 60px rgba(0, 0, 0, 0.55), 0 0 80px rgba(255, 210, 120, 0.25);
}
.unlock-text { display: grid; gap: 0.35rem; justify-items: center; }
.unlock-text p { margin: 0; }
.unlock-kicker {
  font-size: var(--step--1);
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #F2CF7A;
}
.unlock-title { font-family: var(--font-hand); font-weight: 700; font-size: 2.6rem; line-height: 1; }
.unlock-hint { color: rgba(255, 255, 255, 0.72); font-size: var(--step--1); max-width: 26ch; }
.unlock-btn {
  margin-top: 0.75rem;
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}
@media (prefers-reduced-motion: reduce) { .unlock-rays { animation: none; } }
</style>
