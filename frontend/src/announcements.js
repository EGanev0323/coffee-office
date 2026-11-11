/*
  Временни съобщения на началната страница („Ново в системата“).
  Показва се първото, чийто срок не е изтекъл и което колегата не е затворил на това устройство.
  За ново съобщение: добави обект с уникален id и until (кога спира да се показва, с часова зона).
*/
export const ANNOUNCEMENTS = [
  {
    id: 'badges-2026-11',
    until: '2026-11-12T22:00:00+02:00', // 24 часа след пускането на баджовете
    title: 'Ново: баджове!',
    text: 'Изпитите кафета вече носят магнитчета – за 50, 100, 500 и 1000 кафета. Виж колко ти остават до първото в профила.',
    action: { label: 'Виж баджовете', to: '/account' }
  }
]

const KEY = 'coffee.dismissed-announcements'

function dismissedIds() {
  try {
    return JSON.parse(localStorage.getItem(KEY) ?? '[]')
  } catch {
    return []
  }
}

/** Активното съобщение за това устройство или null. */
export function currentAnnouncement(now = Date.now()) {
  const dismissed = dismissedIds()
  return ANNOUNCEMENTS.find((a) => now < new Date(a.until).getTime() && !dismissed.includes(a.id)) ?? null
}

/** „Разбрах“ – съобщението не се показва повече на това устройство. */
export function dismissAnnouncement(id) {
  try {
    const ids = dismissedIds()
    if (!ids.includes(id)) localStorage.setItem(KEY, JSON.stringify([...ids, id].slice(-20)))
  } catch {
    // без localStorage просто ще се покаже пак следващия път
  }
}
