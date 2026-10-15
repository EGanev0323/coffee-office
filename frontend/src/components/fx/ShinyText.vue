<script setup>
/* Текст с лъскава светлина, която минава през него (по ShinyText от React Bits). */
defineProps({
  base: { type: String, default: 'rgba(255, 255, 255, 0.82)' },
  shine: { type: String, default: '#FFFFFF' },
  speed: { type: Number, default: 3 }, // секунди за едно минаване
  disabled: { type: Boolean, default: false }
})
</script>

<template>
  <span
    class="shiny-text"
    :class="{ 'is-disabled': disabled }"
    :style="{ '--shiny-base': base, '--shiny-shine': shine, '--shiny-speed': `${speed}s` }"
  ><slot /></span>
</template>

<style scoped>
.shiny-text {
  color: var(--shiny-base);
  background: linear-gradient(110deg, var(--shiny-base) 38%, var(--shiny-shine) 50%, var(--shiny-base) 62%);
  background-size: 250% 100%;
  background-position: 120% 0;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: shiny-sweep var(--shiny-speed) linear infinite;
}
.shiny-text.is-disabled { animation: none; }
@keyframes shiny-sweep {
  from { background-position: 120% 0; }
  to { background-position: -120% 0; }
}
</style>
