<script setup>
import { computed } from 'vue'
import { markerClass } from '../markers'

/*
  Кръгло аватарче с инициалите на колегата върху неговия цвят на маркер.
  Цветът идва от id-то на колегата ((id − 1) % 6), за да е един и същ навсякъде.
  size: 'md' (по подразбиране) | 'sm'; ring = само контур вместо запълване.
*/
const props = defineProps({
  name: { type: String, default: '' },
  userId: { type: Number, default: null },
  size: { type: String, default: 'md' },
  ring: { type: Boolean, default: false }
})

/** Инициали: първите букви на първите две думи от името. */
const initials = computed(() => {
  const parts = (props.name || '').trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return '—'
  const first = parts[0][0] ?? ''
  const second = parts.length > 1 ? parts[parts.length - 1][0] ?? '' : ''
  return (first + second).toUpperCase()
})
</script>

<template>
  <span
    class="avatar"
    :class="[markerClass(userId), { 'avatar-sm': size === 'sm', 'avatar-ring': ring }]"
    aria-hidden="true"
  >{{ initials }}</span>
</template>
