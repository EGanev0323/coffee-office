<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { gsap, reducedMotion, SplitText } from '../../motion'

/*
  Текст, който се появява буква по буква или дума по дума (по SplitText от React Bits, с GSAP SplitText).
  След анимацията DOM-ът се връща в нормален вид, за да не пречи на четците на екрана и на Vue.
*/
const props = defineProps({
  text: { type: String, required: true },
  tag: { type: String, default: 'p' },
  split: { type: String, default: 'chars' }, // 'chars' | 'words'
  delay: { type: Number, default: 0 },
  stagger: { type: Number, default: 0.03 },
  duration: { type: Number, default: 0.7 },
  ease: { type: String, default: 'back.out(1.7)' },
  from: { type: Object, default: () => ({ yPercent: 100, autoAlpha: 0, rotation: 6 }) }
})

const el = ref(null)
const pending = ref(!reducedMotion())
let split = null
let tween = null

function cleanup() {
  tween?.kill()
  split?.revert()
  tween = null
  split = null
}

async function play() {
  cleanup()
  if (reducedMotion()) {
    pending.value = false
    return
  }
  pending.value = true
  await document.fonts?.ready
  await nextTick()
  const node = el.value
  if (!node) return
  split = SplitText.create(node, {
    type: props.split === 'words' ? 'words' : 'words,chars',
    aria: 'auto'
  })
  const targets = props.split === 'words' ? split.words : split.chars
  pending.value = false
  tween = gsap.from(targets, {
    ...props.from,
    duration: props.duration,
    stagger: props.stagger,
    delay: props.delay,
    ease: props.ease,
    onComplete: () => {
      split?.revert()
      split = null
    }
  })
}

onMounted(play)
watch(() => props.text, play)
onBeforeUnmount(cleanup)
</script>

<template>
  <component :is="tag" :key="text" ref="el" class="split-text" :class="{ 'split-pending': pending }">{{ text }}</component>
</template>

<style scoped>
.split-pending { visibility: hidden; }
</style>
