<script setup>
import { ref, watch } from 'vue'
import { confirmState } from '../confirm'
import { dialogIn, dialogOut, gsap, lockScroll, reducedMotion } from '../motion'

const dialog = ref(null)

watch(() => confirmState.open, (open) => {
  const d = dialog.value
  if (!d) return
  if (open) {
    gsap.killTweensOf(d) // ако се отвори отново, докато още се затваря
    if (!d.open) d.showModal()
    lockScroll(true)
    dialogIn(d)
    if (!reducedMotion()) {
      gsap.from(d.querySelectorAll('.dialog-body > *'), { y: 10, autoAlpha: 0, stagger: 0.05, duration: 0.35, delay: 0.08 })
    }
  }
  if (!open && d.open) {
    dialogOut(d, () => {
      d.close()
      lockScroll(false)
    })
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
  <dialog ref="dialog" autofocus @cancel.prevent="finish(false)" @click.self="finish(false)">
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
