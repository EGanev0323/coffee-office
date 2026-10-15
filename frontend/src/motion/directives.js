/*
  Директиви за движение (Vue версии на ефекти от React Bits):
    v-reveal     – елементът (или децата му) изплуват, когато влязат в екрана (ScrollTrigger)
    v-magnet     – бутонът се „привлича“ към курсора (Magnet)
    v-tilt       – картата се накланя в 3D след курсора (TiltedCard)
    v-spotlight  – мек прожектор следва курсора върху панела (SpotlightCard)
  Магнит, наклон и прожектор работят само с мишка; на телефон и при reduced motion не правят нищо.
*/
import { finePointer, gsap, reducedMotion } from './index'

const state = new WeakMap()

/**
 * v-reveal="{ children: 'li', y: 24, delay: 0, stagger: 0.06, start: 'top 90%' }"
 * Без стойност анимира самия елемент.
 */
const reveal = {
  mounted(el, { value = {} }) {
    if (reducedMotion()) return
    const targets = value.children ? el.querySelectorAll(value.children) : el
    if (value.children && !targets.length) return
    const tween = gsap.from(targets, {
      autoAlpha: 0,
      y: value.y ?? 28,
      x: value.x ?? 0,
      scale: value.scale ?? 1,
      duration: value.duration ?? 0.8,
      delay: value.delay ?? 0,
      stagger: value.stagger ?? 0.06,
      ease: value.ease ?? 'power3.out',
      clearProps: 'transform,opacity,visibility',
      scrollTrigger: { trigger: el, start: value.start ?? 'top 92%', once: true }
    })
    state.set(el, { tween })
  },
  unmounted(el) {
    const s = state.get(el)
    s?.tween?.scrollTrigger?.kill()
    s?.tween?.kill()
    state.delete(el)
  }
}

/** v-magnet или v-magnet="{ strength: 0.35, padding: 40 }" */
const magnet = {
  mounted(el, { value = {} }) {
    if (reducedMotion() || !finePointer()) return
    const strength = value.strength ?? 0.3
    const padding = value.padding ?? 36
    const xTo = gsap.quickTo(el, 'x', { duration: 0.6, ease: 'power3.out' })
    const yTo = gsap.quickTo(el, 'y', { duration: 0.6, ease: 'power3.out' })
    let active = false

    const onMove = (e) => {
      if (el.disabled) {
        if (active) { active = false; xTo(0); yTo(0) }
        return
      }
      const r = el.getBoundingClientRect()
      const cx = r.left + r.width / 2
      const cy = r.top + r.height / 2
      const near = Math.abs(e.clientX - cx) < r.width / 2 + padding && Math.abs(e.clientY - cy) < r.height / 2 + padding
      if (near) {
        active = true
        xTo((e.clientX - cx) * strength)
        yTo((e.clientY - cy) * strength)
      } else if (active) {
        active = false
        xTo(0)
        yTo(0)
      }
    }
    window.addEventListener('pointermove', onMove, { passive: true })
    state.set(el, { cleanup: () => window.removeEventListener('pointermove', onMove) })
  },
  unmounted(el) {
    state.get(el)?.cleanup?.()
    gsap.killTweensOf(el)
    state.delete(el)
  }
}

/** v-tilt или v-tilt="{ max: 10, scale: 1.04 }" – запазва началния наклон (rotate) на елемента. */
const tilt = {
  mounted(el, { value = {} }) {
    if (reducedMotion() || !finePointer()) return
    const max = value.max ?? 10
    const scale = value.scale ?? 1.04
    gsap.set(el, { transformPerspective: 800, transformStyle: 'preserve-3d' })
    const rx = gsap.quickTo(el, 'rotationX', { duration: 0.5, ease: 'power3.out' })
    const ry = gsap.quickTo(el, 'rotationY', { duration: 0.5, ease: 'power3.out' })
    const sc = gsap.quickTo(el, 'scale', { duration: 0.5, ease: 'power3.out' })

    const onMove = (e) => {
      const r = el.getBoundingClientRect()
      const px = (e.clientX - r.left) / r.width - 0.5
      const py = (e.clientY - r.top) / r.height - 0.5
      ry(px * max * 2)
      rx(-py * max * 2)
      sc(scale)
    }
    const onLeave = () => { rx(0); ry(0); sc(1) }
    el.addEventListener('pointermove', onMove)
    el.addEventListener('pointerleave', onLeave)
    state.set(el, {
      cleanup: () => {
        el.removeEventListener('pointermove', onMove)
        el.removeEventListener('pointerleave', onLeave)
      }
    })
  },
  unmounted(el) {
    state.get(el)?.cleanup?.()
    gsap.killTweensOf(el)
    state.delete(el)
  }
}

/** v-spotlight – слага клас .spotlight и подава позицията на курсора в CSS променливи. */
const spotlight = {
  mounted(el) {
    if (!finePointer()) return
    el.classList.add('spotlight')
    const onMove = (e) => {
      const r = el.getBoundingClientRect()
      el.style.setProperty('--spot-x', `${e.clientX - r.left}px`)
      el.style.setProperty('--spot-y', `${e.clientY - r.top}px`)
    }
    el.addEventListener('pointermove', onMove, { passive: true })
    state.set(el, { cleanup: () => el.removeEventListener('pointermove', onMove) })
  },
  unmounted(el) {
    state.get(el)?.cleanup?.()
    state.delete(el)
  }
}

export function installMotionDirectives(app) {
  app.directive('reveal', reveal)
  app.directive('magnet', magnet)
  app.directive('tilt', tilt)
  app.directive('spotlight', spotlight)
}
