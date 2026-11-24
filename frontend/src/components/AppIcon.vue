<script setup>
/*
  Иконите на Кафе дъската (24px мрежа, 2px щрих, объли краища – „рисувани с маркер“).
  Инлайн SVG със stroke: currentColor и клас „ico“, за да взимат цвета на текста и да работят
  и в тъмната тема. До текст – 20px (icon-size), самостоятелно – 24px.
  Иконите, които само подкрепят текст, са aria-hidden; на иконен бутон слагай aria-label отвън.
*/
const props = defineProps({
  name: { type: String, required: true }
})

// контурите (без запълване) от набора в design-system.json / assets/Icons
const ICONS = {
  cup: '<path d="M5 9h11v5a5 5 0 0 1-5 5h-1a5 5 0 0 1-5-5z"/><path d="M16 11h1.5a2.5 2.5 0 0 1 0 5H16"/><path d="M9 3.5v2.5"/><path d="M12.5 3.5v2.5"/>',
  pack: '<path d="M7 4h10l1 4v11a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V8z"/><path d="M6 8h12"/><path d="M12 11.5c-1.6 0-2.5 1.4-2.5 3s.9 3 2.5 3 2.5-1.4 2.5-3-.9-3-2.5-3z"/><path d="M11.3 12c.9 1.5.9 3.5 0 5"/>',
  jar: '<path d="M8 4h8"/><path d="M10 6.5h4"/><path d="M7 4v3.5C5.7 8.6 5 10.1 5 12v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6c0-1.9-.7-3.4-2-4.5V4"/>',
  user: '<circle cx="12" cy="8" r="3.5"/><path d="M5 20c.8-3.6 3.6-5.5 7-5.5s6.2 1.9 7 5.5"/>',
  users: '<circle cx="9" cy="8.5" r="3"/><path d="M3 19.5c.6-3 2.9-4.7 6-4.7s5.4 1.7 6 4.7"/><path d="M16 5.8a3 3 0 0 1 0 5.6"/><path d="M18 14.9c1.6.6 2.6 2.1 3 4.6"/>',
  undo: '<path d="M9 14 4 9l5-5"/><path d="M4 9h10.5a5.5 5.5 0 0 1 0 11H11"/>',
  plus: '<path d="M12 5v14"/><path d="M5 12h14"/>',
  minus: '<path d="M5 12h14"/>',
  trash: '<path d="M4 7h16"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12"/><path d="M9 7V4h6v3"/>',
  marker: '<path d="M14.5 4.5l5 5L9 20H4v-5z"/><path d="M12.5 6.5l5 5"/>',
  eraser: '<path d="M8 20h12"/><path d="M4.7 14.3l9-9a2 2 0 0 1 2.8 0l2.2 2.2a2 2 0 0 1 0 2.8L11 18H8.4z"/><path d="M9.5 9.5l5 5"/>',
  'chevron-left': '<path d="M15 5l-7 7 7 7"/>',
  'chevron-right': '<path d="M9 5l7 7-7 7"/>',
  check: '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
  close: '<path d="M6 6l12 12"/><path d="M18 6 6 18"/>',
  bell: '<path d="M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z"/><path d="M10 20.5a2 2 0 0 0 4 0"/>',
  chart: '<path d="M4 4v16h16"/><path d="M8 16v-4"/><path d="M12 16V8"/><path d="M16 16v-6"/>',
  calendar: '<rect x="4" y="5.5" width="16" height="14.5" rx="2"/><path d="M4 10h16"/><path d="M8 3.5v4"/><path d="M16 3.5v4"/>',
  clock: '<circle cx="12" cy="12" r="8"/><path d="M12 8v4l3 2"/>',
  logout: '<path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4"/><path d="M10 16l-4-4 4-4"/><path d="M6 12h10"/>',
  magnet: '<path d="M6 4v8a6 6 0 0 0 12 0V4h-4v8a2 2 0 0 1-4 0V4z"/><path d="M6 8h4"/><path d="M14 8h4"/>',
  sticky: '<path d="M5 4h14v10l-6 6H5z"/><path d="M13 20v-6h6"/>',
  tally: '<path d="M6 5v14"/><path d="M10 5v14"/><path d="M14 5v14"/><path d="M18 5v14"/><path d="M4 16l16-8"/>',
  admin: '<path d="M12 3l7 3v5c0 4.5-3 8.3-7 10-4-1.7-7-5.5-7-10V6z"/><path d="M9 12l2 2 4-4"/>'
}

const paths = ICONS[props.name] ?? ''
</script>

<template>
  <svg class="ico" viewBox="0 0 24 24" aria-hidden="true" v-html="paths" />
</template>
