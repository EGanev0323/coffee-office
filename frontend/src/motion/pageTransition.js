/*
  Преход между страниците: старата избледнява нагоре, новата изплува отдолу.
  Ползва се като JS hooks на <Transition :css="false">.
*/
import { afterTick, gsap, reducedMotion, scrollToTop, ScrollTrigger } from './index'

export function pageLeave(el, done) {
  if (reducedMotion()) return afterTick(done)
  gsap.to(el, { autoAlpha: 0, y: -14, duration: 0.22, ease: 'power2.in', onComplete: done })
}

export function pageEnter(el, done) {
  scrollToTop()
  if (reducedMotion()) return done()
  gsap.fromTo(
    el,
    { autoAlpha: 0, y: 26 },
    {
      autoAlpha: 1,
      y: 0,
      duration: 0.55,
      ease: 'power3.out',
      clearProps: 'transform,opacity,visibility',
      onComplete: () => {
        ScrollTrigger.refresh()
        done()
      }
    }
  )
}
