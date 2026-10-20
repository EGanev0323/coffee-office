/*
  Анимационният слой на Кафе дъската: GSAP (+ ScrollTrigger, SplitText) и Lenis за плавно скролване.
  Всичко тук уважава prefers-reduced-motion – тогава няма Lenis и анимациите показват крайното състояние веднага.
*/
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { SplitText } from 'gsap/SplitText'
import Lenis from 'lenis'
import 'lenis/dist/lenis.css'

gsap.registerPlugin(ScrollTrigger, SplitText)
gsap.defaults({ ease: 'power3.out', duration: 0.6 })

const reduceQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
const fineQuery = window.matchMedia('(hover: hover) and (pointer: fine)')

/** Потребителят е поискал по-малко движение. */
export const reducedMotion = () => reduceQuery.matches
const phoneQuery = window.matchMedia('(max-width: 640px)')
/** Тесен екран – диалозите са „лист“ отдолу. */
export const isPhone = () => phoneQuery.matches

/** Кратка вибрация като потвърждение (Android; iPhone я пропуска). */
export function haptic(pattern = 18) {
  try { navigator.vibrate?.(pattern) } catch { /* не е поддържано */ }
}

/** Мишка (не телефон) – за магнитни бутони, наклон и прожектор. */
export const finePointer = () => fineQuery.matches

/** Цветовете на маркерите – същите като в бекенда. */
export const MARKER_COLORS = ['#1E4BAF', '#B8372A', '#2B1F18']

let lenis = null

function tick(time) {
  lenis?.raf(time * 1000)
}

/** Плавно скролване с Lenis, вързано към тикера на GSAP, за да вървят заедно със ScrollTrigger. */
export function initSmoothScroll() {
  if (lenis || reducedMotion()) return lenis
  lenis = new Lenis({
    autoRaf: false,
    lerp: 0.11,
    // в диалози и елементи със собствено скролване (широки таблици) колелцето остава нативно
    prevent: (node) => !!node.closest?.('dialog, [data-lenis-prevent]')
  })
  lenis.on('scroll', ScrollTrigger.update)
  gsap.ticker.add(tick)
  gsap.ticker.lagSmoothing(0)
  return lenis
}

function destroySmoothScroll() {
  if (!lenis) return
  gsap.ticker.remove(tick)
  lenis.destroy()
  lenis = null
}

// Ако настройката се смени, докато страницата е отворена.
reduceQuery.addEventListener?.('change', (e) => {
  if (e.matches) destroySmoothScroll()
  else initSmoothScroll()
})

export const getLenis = () => lenis

/** Спира скролването на страницата (напр. докато е отворен диалог). */
export function lockScroll(locked) {
  if (!lenis) return
  if (locked) lenis.stop()
  else lenis.start()
}

export function scrollToTop() {
  if (lenis) lenis.scrollTo(0, { immediate: true, force: true })
  else window.scrollTo(0, 0)
}

/** Появата на диалог: на телефон се плъзга отдолу, на компютър изскача в средата. */
export function dialogIn(el) {
  if (!el || reducedMotion()) return
  gsap.killTweensOf(el)
  if (isPhone()) {
    gsap.fromTo(el, { yPercent: 100 }, { yPercent: 0, duration: 0.45, ease: 'power3.out', clearProps: 'transform' })
  } else {
    gsap.fromTo(el, { y: 24, scale: 0.92, autoAlpha: 0 }, { y: 0, scale: 1, autoAlpha: 1, duration: 0.45, ease: 'back.out(1.7)', clearProps: 'transform,opacity,visibility' })
  }
}

/** Скриване на диалог; onDone затваря самия <dialog>. */
export function dialogOut(el, onDone) {
  if (!el || reducedMotion()) return onDone()
  gsap.killTweensOf(el)
  const vars = isPhone()
    ? { yPercent: 100, duration: 0.25, ease: 'power2.in' }
    : { y: 12, scale: 0.96, autoAlpha: 0, duration: 0.18, ease: 'power2.in' }
  gsap.to(el, { ...vars, onComplete: () => { onDone(); gsap.set(el, { clearProps: 'all' }) } })
}

/**
 * Извиква done на следващия микротакт. Синхронно done() в leave hook при mode="out-in"
 * чупи Vue (новият компонент се вмъква, докато старият още се маха).
 */
export function afterTick(done) {
  Promise.resolve().then(done)
}

/** Кратко „не“ разклащане – при грешка във форма. */
export function shake(el) {
  if (!el || reducedMotion()) return
  gsap.fromTo(
    el,
    { x: 0 },
    {
      keyframes: { x: [0, -10, 9, -7, 5, -3, 0] },
      duration: 0.5,
      ease: 'power1.inOut',
      clearProps: 'transform'
    }
  )
}

/** „Подскок“ на число или бутон след промяна. */
export function pop(el, scale = 1.18) {
  if (!el || reducedMotion()) return
  gsap.fromTo(el, { scale }, { scale: 1, duration: 0.7, ease: 'elastic.out(1, 0.45)', clearProps: 'transform' })
}

export { gsap, ScrollTrigger, SplitText }
