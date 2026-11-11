/*
  Баджовете за изпити кафета. Кодовете са същите като в бекенда (domain/Badge.java),
  а картинките са кръгли „магнитчета“ в assets/badges/.
*/
import { reactive } from 'vue'
import { api } from './api'
import cups50 from './assets/badges/cups-50.webp'
import cups100 from './assets/badges/cups-100.webp'
import cups500 from './assets/badges/cups-500.webp'
import cups1000 from './assets/badges/cups-1000.webp'

export const BADGES = {
  CUPS_50: { img: cups50, title: '50 кафета', tier: 'Бронз' },
  CUPS_100: { img: cups100, title: '100 кафета', tier: 'Сребро' },
  CUPS_500: { img: cups500, title: '500 кафета', tier: 'Злато' },
  CUPS_1000: { img: cups1000, title: '1000 кафета', tier: 'Космос' }
}

export const badgeImg = (code) => BADGES[code]?.img
export const badgeTitle = (code) => BADGES[code]?.title ?? code

/*
  Отключване на нов бадж: след „Изпих кафе“ страницата вика checkNewBadges(),
  а App.vue показва unlocked.code (след плаката „Да ти е сладко…“).
*/
export const unlocked = reactive({ code: null, delay: 0 })

/** Баджовете на влезлия колега (за профила). */
export function loadBadges() {
  return api('/me/badges')
}

/**
 * Проверява дали току-що изпитото кафе е отключило бадж – тогава прагът му е точно
 * колкото са изпитите кафета. delayMs: колко да изчака, преди да го покаже (докато е плакатът).
 */
export async function checkNewBadges(delayMs = 0) {
  try {
    const data = await loadBadges()
    const badge = data.badges.find((b) => b.earnedAt && b.threshold === data.coffees)
    if (!badge) return
    unlocked.delay = delayMs
    unlocked.code = badge.code
  } catch {
    // баджът ще се види на дъската и в профила – тук не е важно
  }
}
