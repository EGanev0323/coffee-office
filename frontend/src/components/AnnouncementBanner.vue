<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { BADGES } from '../badges'
import { currentAnnouncement, dismissAnnouncement } from '../announcements'
import { afterTick, gsap, reducedMotion } from '../motion'

/*
  Временно съобщение най-горе на началната страница – сега за баджовете.
  Изчезва само след срока си или когато колегата натисне „×“ / бутона.
*/
const emit = defineEmits(['visible'])
const item = ref(currentAnnouncement())
const magnets = Object.values(BADGES).map((b) => b.img)
let expiryTimer

function hide() {
  if (item.value) dismissAnnouncement(item.value.id)
  item.value = null // мястото в мрежата се освобождава след анимацията (after-leave)
}

onMounted(() => {
  emit('visible', !!item.value)
  if (item.value) {
    // ако страницата стои отворена, когато срокът изтече
    const left = new Date(item.value.until).getTime() - Date.now()
    if (left > 0 && left < 2 ** 31 - 1) expiryTimer = setTimeout(() => { item.value = null }, left)
  }
})
onBeforeUnmount(() => clearTimeout(expiryTimer))

function onEnter(el, done) {
  if (reducedMotion()) return done()
  gsap
    .timeline({ onComplete: done, delay: 0.35 })
    .from(el, { y: -16, autoAlpha: 0, duration: 0.5, ease: 'power3.out', clearProps: 'transform,opacity,visibility' })
    .from(el.querySelectorAll('.notice-magnet'), {
      y: -26, scale: 1.8, rotation: -30, autoAlpha: 0, duration: 0.55, stagger: 0.09, ease: 'back.out(2.4)', clearProps: 'transform,opacity,visibility'
    }, 0.2)
}
function onLeave(el, done) {
  if (reducedMotion()) return afterTick(done)
  gsap.to(el, { height: 0, autoAlpha: 0, marginBottom: 0, paddingBlock: 0, duration: 0.35, ease: 'power2.in', onComplete: done })
}
</script>

<template>
  <Transition :css="false" appear @enter="onEnter" @leave="onLeave" @after-leave="emit('visible', false)">
    <aside v-if="item" class="notice" role="status" aria-live="polite">
      <div class="notice-magnets" aria-hidden="true">
        <img v-for="(src, i) in magnets" :key="i" :src="src" alt="" class="notice-magnet" width="34" height="34" draggable="false" />
      </div>
      <div class="notice-body">
        <p class="notice-title">{{ item.title }}</p>
        <p class="notice-text">{{ item.text }}</p>
        <RouterLink v-if="item.action" :to="item.action.to" class="btn btn-primary btn-small notice-action" @click="hide">
          {{ item.action.label }}
        </RouterLink>
      </div>
      <button type="button" class="notice-close" aria-label="Затвори съобщението" @click="hide">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
      </button>
    </aside>
  </Transition>
</template>

<style scoped>
.notice {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0.9rem;
  align-items: start;
  padding: 1rem 2.75rem 1rem 1rem;
  border-radius: 16px;
  background: linear-gradient(135deg, #24203A, #3A2D5C 60%, #4B3570);
  color: #fff;
  box-shadow: 0 14px 30px -18px rgba(43, 31, 24, 0.7);
  overflow: hidden;
}
/* четирите магнитчета едно върху друго */
.notice-magnets { display: flex; padding-top: 0.15rem; }
.notice-magnet {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  margin-left: -12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.45), inset 0 0 0 1px rgba(255, 255, 255, 0.2);
}
.notice-magnet:first-child { margin-left: 0; }
.notice-magnet:nth-child(odd) { rotate: -8deg; }
.notice-magnet:nth-child(even) { rotate: 6deg; }
.notice-body { display: grid; gap: 0.3rem; justify-items: start; min-width: 0; }
.notice-body p { margin: 0; }
.notice-title { font-weight: 700; font-size: var(--step-0); color: #F2CF7A; }
.notice-text { font-size: var(--step--1); line-height: 1.45; color: rgba(255, 255, 255, 0.85); }
.notice-action { margin-top: 0.4rem; text-decoration: none; }
.notice-close {
  position: absolute;
  top: 0.35rem;
  right: 0.35rem;
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
}
.notice-close:active { background: rgba(255, 255, 255, 0.12); }
.notice-close svg { width: 20px; height: 20px; fill: none; stroke: currentColor; stroke-width: 2.2; stroke-linecap: round; }

@media (max-width: 420px) {
  .notice { grid-template-columns: minmax(0, 1fr); }
}
</style>
