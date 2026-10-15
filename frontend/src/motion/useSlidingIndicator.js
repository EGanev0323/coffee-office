import { nextTick, onBeforeUnmount, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { gsap, reducedMotion } from './index'

/**
 * Индикатор (хапче или черта), който се плъзга под активния линк в навигация.
 * Навигацията трябва да е position: relative.
 */
export function useSlidingIndicator(navRef, indicatorRef, { selector = 'a.is-active' } = {}) {
  const route = useRoute()
  let observer = null
  let first = true

  function move(animate = true) {
    const nav = navRef.value
    const ind = indicatorRef.value
    if (!nav || !ind) return
    gsap.killTweensOf(ind) // иначе недовършено скриване може да „надвие“ показването
    const active = nav.querySelector(selector)
    if (!active) {
      gsap.to(ind, { autoAlpha: 0, duration: 0.2 })
      first = true
      return
    }
    const props = {
      x: active.offsetLeft,
      y: active.offsetTop,
      width: active.offsetWidth,
      height: active.offsetHeight,
      autoAlpha: 1
    }
    if (!animate || first || reducedMotion()) {
      gsap.set(ind, props)
      first = false
    } else {
      gsap.to(ind, { ...props, duration: 0.55, ease: 'elastic.out(1, 0.75)' })
    }
  }

  // fullPath може да не се промени при първата навигация ('/' → '/'), затова следим и съвпадналите маршрути
  watch(
    () => `${route.fullPath}|${route.matched.map((r) => r.path).join(',')}`,
    () => nextTick(() => move(true))
  )

  // навигацията може да се появи по-късно (v-if след зареждане на потребителя)
  watch(navRef, (el, old) => {
    if (old) observer?.unobserve(old)
    if (el) {
      first = true
      observer?.observe(el)
      nextTick(() => move(false))
    }
  })

  onMounted(() => {
    // шрифтовете и пренасянето на редове на телефон променят размерите
    observer = new ResizeObserver(() => move(false))
    if (navRef.value) observer.observe(navRef.value)
    nextTick(() => move(false))
    document.fonts?.ready.then(() => move(false))
  })
  onBeforeUnmount(() => observer?.disconnect())

  return { move }
}
