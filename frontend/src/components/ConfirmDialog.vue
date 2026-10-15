<script setup>
import { ref, watch } from 'vue'
import { confirmState } from '../confirm'
import { gsap, lockScroll, reducedMotion } from '../motion'

const dialog = ref(null)

watch(() => confirmState.open, (open) => {
  const d = dialog.value
  if (!d) return
  if (open) {
    gsap.killTweensOf(d) // ако се отвори отново, докато още се затваря
    if (!d.open) d.showModal()
    lockScroll(true)
    if (!reducedMotion()) {
      gsap.fromTo(d, { y: 24, scale: 0.92, autoAlpha: 0 }, { y: 0, scale: 1, autoAlpha: 1, duration: 0.45, ease: 'back.out(1.7)' })
      gsap.from(d.querySelectorAll('.dialog-body > *'), { y: 10, autoAlpha: 0, stagger: 0.05, duration: 0.35, delay: 0.08 })
    }
  }
  if (!open && d.open) {
    const close = () => {
      d.close()
      gsap.set(d, { clearProps: 'all' })
      lockScroll(false)
    }
    if (reducedMotion()) close()
    else gsap.to(d, { y: 12, scale: 0.96, autoAlpha: 0, duration: 0.18, ease: 'power2.in', onComplete: close })
  }
})

function finish(result) {
  const resolve = confirmState.resolve
  confirmState.open = false
  confirmState.resolve = null
  resolve?.(result)
}
</script>

<template>
  <dialog ref="dialog" @cancel.prevent="finish(false)">
    <div class="dialog-body">
      <h2>{{ confirmState.title }}</h2>
      <p v-if="confirmState.message">{{ confirmState.message }}</p>
      <div class="dialog-actions">
        <button type="button" class="btn btn-quiet" @click="finish(false)">Откажи</button>
        <button
          type="button"
          class="btn"
          :class="confirmState.danger ? 'btn-danger-fill' : 'btn-primary'"
          @click="finish(true)"
        >{{ confirmState.confirmText }}</button>
      </div>
    </div>
  </dialog>
</template>
