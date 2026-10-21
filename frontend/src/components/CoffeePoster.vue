<script setup>
import { computed } from 'vue'
import poster from '../assets/coffee-poster.jpg'

/*
  Плакатът „Да ти е сладко…“, който излиза след изпито кафе.
  Надписът е истински текст върху картината (не е част от снимката), затова е остър на всеки екран.
  Редовете имат клас .poster-line – родителят ги анимира като печат.
*/
const props = defineProps({
  count: { type: Number, default: 1 } // кое кафе за днес е това
})

const VARIANTS = {
  first: {
    text: 'Да ти е сладко първото кафе!',
    lines: [['Да ти е', 'sm'], ['Сладко', 'lg'], ['bar'], ['първото', 'sm'], ['Кафе!', 'lg']]
  },
  second: {
    text: 'Да ти е сладко второто кафе!',
    lines: [['Да ти е', 'sm'], ['Сладко', 'lg'], ['bar'], ['второто', 'sm'], ['Кафе!', 'lg']]
  },
  many: {
    text: 'Да ти е сладко, ама много кафе пиеш!',
    lines: [['Да ти е', 'sm'], ['Сладко,', 'lg'], ['bar'], ['ама много', 'sm'], ['Кафе', 'md'], ['пиеш!', 'md']]
  }
}

const variant = computed(() => {
  if (props.count >= 3) return VARIANTS.many
  return props.count === 2 ? VARIANTS.second : VARIANTS.first
})

defineExpose({ text: computed(() => variant.value.text) })
</script>

<template>
  <figure class="poster" :aria-label="variant.text">
    <img :src="poster" alt="" class="poster-img" width="672" height="1060" draggable="false" />
    <figcaption class="poster-text" aria-hidden="true">
      <template v-for="(line, i) in variant.lines" :key="i">
        <span v-if="line[0] === 'bar'" class="poster-line poster-bar"></span>
        <span v-else class="poster-line" :class="`is-${line[1]}`">{{ line[0] }}</span>
      </template>
    </figcaption>
    <span class="visually-hidden">{{ variant.text }}</span>
  </figure>
</template>

<style scoped>
.poster {
  position: relative;
  margin: 0;
  container-type: inline-size;
  aspect-ratio: 672 / 1060;
  overflow: hidden;
  border-radius: 2px;
  background: #B48F61;
}
.poster-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  user-select: none;
}
/* надписът в горния десен ъгъл, както на оригиналния плакат */
.poster-text {
  position: absolute;
  top: 4cqw;
  right: 2.5cqw;
  width: 41cqw; /* вдясно от главата, както „Ти изпи ли си кафето?“ на оригинала */
  display: grid;
  justify-items: center;
  text-align: center;
  font-family: 'Oswald', 'Arial Narrow', 'Roboto Condensed', sans-serif;
  font-weight: 700;
  line-height: 0.92;
  color: #8A2418;
  mix-blend-mode: multiply; /* „мастило“ върху хартията */
  transform: rotate(-1.5deg);
  letter-spacing: -0.01em;
}
.poster-line { display: block; }
.is-sm { font-size: 6.8cqw; font-weight: 600; margin: 0.8cqw 0; }
.is-md { font-size: 10.5cqw; }
.is-lg { font-size: 11.5cqw; }
.poster-bar {
  width: 84%;
  height: 1.5cqw;
  margin: 2.6cqw 0 0.8cqw;
  background: currentColor;
  transform: rotate(-4deg);
  border-radius: 1px;
}
.visually-hidden {
  position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap;
}
</style>
