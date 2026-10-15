<script setup>
import { onMounted, onUnmounted } from 'vue'
import photo from '../assets/coffee-face.jpg'
import { gsap, reducedMotion, SplitText, afterTick } from '../motion'
import { burst } from '../motion/burst'

/* Снимка в средата на екрана след „Изпих кафе“. Затваря се сама, с клик или с Esc. */
const props = defineProps({
  show: { type: Boolean, default: false },
  message: { type: String, default: 'Ти изпи кафе' },
  warning: { type: Boolean, default: false }
})
const emit = defineEmits(['close'])

function onKey(e) {
  if (props.show && e.key === 'Escape') emit('close')
}
onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => window.removeEventListener('keydown', onKey))

/* Снимката „пада“ и се закача на дъската, надписът се изписва буква по буква, а зърната се разхвърчават. */
function onEnter(el, done) {
  if (reducedMotion()) return done()
  const card = el.querySelector('.drink-card')
  const caption = el.querySelector('.drink-caption')
  const split = SplitText.create(caption, { type: 'chars', aria: 'auto' })

  gsap
    .timeline({ onComplete: () => { split.revert(); done() } })
    .fromTo(el, { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.25, ease: 'power1.out' })
    .fromTo(
      card,
      { y: -160, rotation: -22, scale: 0.6 },
      { y: 0, rotation: -2, scale: 1, duration: 0.9, ease: 'elastic.out(1, 0.55)' },
      0
    )
    .add(() => burst(card, { kind: 'beans', count: 18, power: 1.15 }), 0.28)
    .from(
      split.chars,
      { yPercent: 80, autoAlpha: 0, rotation: () => gsap.utils.random(-25, 25), duration: 0.5, stagger: 0.035, ease: 'back.out(2.2)' },
      0.35
    )
    .to(card, props.warning
      ? { keyframes: { rotation: [-2, -6, 3, -5, 1, -2] }, duration: 0.6, ease: 'power1.inOut' }
      : { scale: 1.03, yoyo: true, repeat: 1, duration: 0.25, ease: 'sine.inOut' })
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
        <figure class="drink-card">
          <span class="drink-pin" aria-hidden="true"></span>
          <img :src="photo" alt="" class="drink-photo" width="280" height="280" />
          <figcaption class="drink-caption" :class="{ 'is-warning': warning }">{{ message }}</figcaption>
        </figure>
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
  padding: 1rem;
  background: rgba(43, 31, 24, 0.38);
  -webkit-backdrop-filter: blur(3px);
  backdrop-filter: blur(3px);
  cursor: pointer;
}
/* като снимка, закачена на дъската */
.drink-card {
  position: relative;
  margin: 0;
  background: #FFFFFF;
  padding: 14px 14px 10px;
  border-radius: 6px;
  box-shadow: 0 18px 50px rgba(43, 31, 24, 0.3);
  transform: rotate(-2deg);
  text-align: center;
}
/* магнитче отгоре */
.drink-pin {
  position: absolute;
  top: -9px;
  left: 50%;
  width: 22px;
  height: 22px;
  margin-left: -11px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #E46A5C, var(--red) 60%, #8A251B);
  box-shadow: 0 3px 6px rgba(43, 31, 24, 0.35);
}
.drink-photo {
  display: block;
  width: min(280px, 70vw);
  height: auto;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 2px;
}
.drink-caption {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 2.2rem;
  line-height: 1.1;
  color: var(--marker);
  margin-top: 0.5rem;
  max-width: min(280px, 70vw);
  margin-inline: auto;
}
.drink-caption.is-warning { color: var(--red); }
</style>
