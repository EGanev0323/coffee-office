/*
  Изстрелване на малки фигурки от една точка – кафе зърна, когато изпиеш кафе,
  и чертички с маркер, когато купиш пакет. Летят нагоре, въртят се и падат с гравитация.
*/
import { gsap, MARKER_COLORS, reducedMotion } from './index'

export const BEAN = `<svg viewBox="0 0 20 26" width="100%" height="100%" aria-hidden="true">
  <ellipse cx="10" cy="13" rx="8.5" ry="11.5" fill="#6B4630"/>
  <path d="M10 2.5c-3.2 4-3.2 8 0 10.5s3.2 6.5 0 10.5" fill="none" stroke="#3A2416" stroke-width="1.8" stroke-linecap="round"/>
</svg>`

const strokeSvg = (color) => `<svg viewBox="0 0 8 30" width="100%" height="100%" aria-hidden="true">
  <line x1="4" y1="3" x2="4.6" y2="27" stroke="${color}" stroke-width="3.4" stroke-linecap="round"/>
</svg>`

let layer = null
function getLayer() {
  if (layer?.isConnected) return layer
  layer = document.createElement('div')
  layer.className = 'fx-burst-layer'
  layer.setAttribute('aria-hidden', 'true')
  document.body.appendChild(layer)
  return layer
}

function originOf(from) {
  if (!from) return { x: window.innerWidth / 2, y: window.innerHeight / 2 }
  if (typeof from.x === 'number') return from
  const r = (from.getBoundingClientRect ? from : from.currentTarget)?.getBoundingClientRect?.()
  return r ? { x: r.left + r.width / 2, y: r.top + r.height / 2 } : { x: window.innerWidth / 2, y: window.innerHeight / 2 }
}

/**
 * @param from  елемент, DOMRect-подобен обект {x, y} или събитие
 * @param opts  { kind: 'beans' | 'strokes', count, power, colors }
 */
export function burst(from, opts = {}) {
  if (reducedMotion()) return
  const { kind = 'beans', count = kind === 'beans' ? 16 : 14, power = 1, colors = MARKER_COLORS } = opts
  const { x, y } = originOf(from)
  const root = getLayer()

  for (let i = 0; i < count; i++) {
    const piece = document.createElement('span')
    piece.className = 'fx-burst-piece'
    const size = kind === 'beans' ? gsap.utils.random(12, 22) : gsap.utils.random(9, 13)
    piece.style.width = `${size}px`
    piece.style.height = `${kind === 'beans' ? size * 1.3 : size * 3.2}px`
    piece.innerHTML = kind === 'beans' ? BEAN : strokeSvg(colors[i % colors.length])
    root.appendChild(piece)

    const angle = gsap.utils.random(-Math.PI * 0.92, -Math.PI * 0.08) // ветрило нагоре
    const speed = gsap.utils.random(140, 340) * power
    const dx = Math.cos(angle) * speed
    const rise = Math.sin(angle) * speed
    const fall = gsap.utils.random(260, 480) * power
    const life = gsap.utils.random(0.9, 1.4)

    gsap.set(piece, { x, y, xPercent: -50, yPercent: -50, rotation: gsap.utils.random(-90, 90), scale: 0 })
    const tl = gsap.timeline({ onComplete: () => piece.remove() })
    tl.to(piece, { scale: 1, duration: 0.18, ease: 'back.out(3)' }, 0)
      .to(piece, { x: x + dx * 1.6, duration: life, ease: 'power1.out' }, 0)
      .to(piece, { y: y + rise, duration: life * 0.4, ease: 'power2.out' }, 0)
      .to(piece, { y: y + rise + fall, duration: life * 0.6, ease: 'power2.in' }, life * 0.4)
      .to(piece, { rotation: `+=${gsap.utils.random(-540, 540)}`, duration: life, ease: 'none' }, 0)
      .to(piece, { autoAlpha: 0, duration: 0.3 }, life - 0.3)
  }
}
