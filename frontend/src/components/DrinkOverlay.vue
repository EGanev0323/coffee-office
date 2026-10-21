<script setup>
import { onMounted, onUnmounted } from 'vue'
import CoffeePoster from './CoffeePoster.vue'
import { afterTick, gsap, reducedMotion } from '../motion'
import { posterIntro } from '../motion/poster'

/* Плакатът „Да ти е сладко…“ в средата на екрана след „Изпих кафе“. Затваря се сам, с докосване или с Esc. */
const props = defineProps({
  show: { type: Boolean, default: false },
  count: { type: Number, default: 1 } // кое кафе за днес е това
})
const emit = defineEmits(['close'])

function onKey(e) {
  if (props.show && e.key === 'Escape') emit('close')
}
onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => window.removeEventListener('keydown', onKey))

function onEnter(el, done) {
  if (reducedMotion()) return done()
  const card = el.querySelector('.drink-card')
  gsap
    .timeline({ onComplete: done })
    .fromTo(el, { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.25, ease: 'power1.out' })
    .add(posterIntro(card, { warning: props.count >= 3 }), 0)
}

function onLeave(el, done) {
  if (reducedMotion()) return afterTick(done)
  const card = el.querySelector('.drink-card')
  gsap.killTweensOf([el, card]) // затворено, преди появата да е свършила
  gsap
    .timeline({ onComplete: done })
    .to(card, { y: 40, rotation: 6, scale: 0.85, autoAlpha: 0, duration: 0.28, ease: 'power2.in' })
    .to(el, { autoAlpha: 0, duration: 0.2 }, '-=0.12')
}
</script>

<template>
  <Teleport to="body">
    <Transition :css="false" @enter="onEnter" @leave="onLeave">
      <div v-if="show" class="drink-overlay" role="status" aria-live="polite" @click="emit('close')">
        <div class="drink-card">
          <span class="drink-pin" aria-hidden="true"></span>
          <CoffeePoster :count="count" />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.drink-overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: max(1rem, env(safe-area-inset-top, 0px)) 1rem max(1rem, env(safe-area-inset-bottom, 0px));
  background: rgba(43, 31, 24, 0.45);
  -webkit-backdrop-filter: blur(3px);
  backdrop-filter: blur(3px);
  cursor: pointer;
}
/* плакатът, закачен на дъската с магнитче */
.drink-card {
  position: relative;
  width: min(80vw, 340px, calc((100dvh - 4rem) * 0.634));
  padding: 6px;
  background: #F4ECDD;
  border-radius: 4px;
  box-shadow: 0 22px 55px rgba(43, 31, 24, 0.4);
  transform: rotate(-2deg);
}
.drink-pin {
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
</style>
