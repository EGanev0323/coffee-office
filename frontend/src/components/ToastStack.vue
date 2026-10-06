<script setup>
import { toasts } from '../notify'
</script>

<template>
  <div class="toasts" role="status" aria-live="polite">
    <div v-for="t in toasts" :key="t.id" class="toast" :class="{ 'toast-error': t.type === 'error' }">
      {{ t.message }}
    </div>
  </div>
</template>
