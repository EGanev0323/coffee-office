<script setup>
import { ref, watch } from 'vue'
import { confirmState } from '../confirm'

const dialog = ref(null)

watch(() => confirmState.open, (open) => {
  if (!dialog.value) return
  if (open && !dialog.value.open) dialog.value.showModal()
  if (!open && dialog.value.open) dialog.value.close()
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
