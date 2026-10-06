<script setup>
import { onMounted, onUnmounted } from 'vue'
import photo from '../assets/coffee-face.jpg'

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
</script>

<template>
  <Teleport to="body">
    <Transition name="drink-pop" :duration="{ enter: 380, leave: 250 }">
      <div v-if="show" class="drink-overlay" role="status" aria-live="polite" @click="emit('close')">
        <figure class="drink-card">
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
  background: rgba(43, 31, 24, 0.35);
  cursor: pointer;
}
/* като снимка, закачена на дъската */
.drink-card {
  margin: 0;
  background: #FFFFFF;
  padding: 14px 14px 10px;
  border-radius: 6px;
  box-shadow: 0 18px 50px rgba(43, 31, 24, 0.3);
  transform: rotate(-2deg);
  text-align: center;
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

.drink-pop-enter-active,
.drink-pop-leave-active { transition: opacity 0.25s ease; }
.drink-pop-enter-from,
.drink-pop-leave-to { opacity: 0; }
.drink-pop-enter-active .drink-card { transition: transform 0.38s cubic-bezier(0.2, 1.4, 0.4, 1); }
.drink-pop-enter-from .drink-card { transform: scale(0.8) rotate(-8deg); }
</style>