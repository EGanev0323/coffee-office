/*
  Появата на плаката след изпито кафе: пада и се закача с магнитче, хвърчат зърна,
  а редовете на надписа се „отпечатват“ един след друг като с печат.
*/
import { gsap } from './index'
import { burst } from './burst'

/**
 * @param card  елементът, който пада (рамката на плаката)
 * @param opts  { warning } – при „много кафе“ плакатът се разклаща накрая
 * @returns gsap timeline (без reduced motion проверка – родителят я прави)
 */
export function posterIntro(card, { warning = false } = {}) {
  const lines = card.querySelectorAll('.poster-line')
  const tl = gsap.timeline()

  tl.fromTo(
    card,
    { y: -180, rotation: -22, scale: 0.6, autoAlpha: 0 },
    { y: 0, rotation: -2, scale: 1, autoAlpha: 1, duration: 0.9, ease: 'elastic.out(1, 0.55)' },
    0
  )
    .add(() => burst(card, { kind: 'beans', count: 18, power: 1.15 }), 0.28)
    .fromTo(
      lines,
      { scale: 2.4, autoAlpha: 0, rotation: () => gsap.utils.random(-10, 10) },
      { scale: 1, autoAlpha: 1, rotation: 0, duration: 0.32, stagger: 0.13, ease: 'power4.in', clearProps: 'transform' },
      0.45
    )

  // всеки удар на печата леко разтърсва плаката
  lines.forEach((_, i) => {
    tl.to(card, { y: 3, duration: 0.05, yoyo: true, repeat: 1, ease: 'power1.out' }, 0.45 + 0.32 + i * 0.13)
  })

  tl.to(
    card,
    warning
      ? { keyframes: { rotation: [-2, -6, 3, -5, 1, -2] }, duration: 0.6, ease: 'power1.inOut' }
      : { scale: 1.03, yoyo: true, repeat: 1, duration: 0.25, ease: 'sine.inOut' }
  )
  return tl
}
