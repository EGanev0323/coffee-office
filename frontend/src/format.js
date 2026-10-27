const moneyFmt = new Intl.NumberFormat('bg-BG', { style: 'currency', currency: 'EUR' })
const dateTimeFmt = new Intl.DateTimeFormat('bg-BG', {
  day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
})
const timeFmt = new Intl.DateTimeFormat('bg-BG', { hour: '2-digit', minute: '2-digit' })
const dayFmt = new Intl.DateTimeFormat('bg-BG', { weekday: 'long', day: 'numeric', month: 'long' })
const dayYearFmt = new Intl.DateTimeFormat('bg-BG', { day: 'numeric', month: 'long', year: 'numeric' })
const monthFmt = new Intl.DateTimeFormat('bg-BG', { month: 'long', year: 'numeric' })

export const money = (v) => moneyFmt.format(Number(v ?? 0))
export const dateTime = (v) => dateTimeFmt.format(new Date(v))
export const time = (v) => timeFmt.format(new Date(v))
/** „Днес“, „Вчера“ или „понеделник, 26 октомври“ – заглавие на ден в историята. */
export function dayLabel(v) {
  const d = new Date(v)
  const today = new Date()
  const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1)
  if (d.toDateString() === today.toDateString()) return 'Днес'
  if (d.toDateString() === yesterday.toDateString()) return 'Вчера'
  return (d.getFullYear() === today.getFullYear() ? dayFmt : dayYearFmt).format(d)
}
export const coffees = (n) => (n === 1 ? '1 кафе' : `${n} кафета`)
export const monthLabel = (year, month) => monthFmt.format(new Date(year, month - 1, 1)).replace(/\s*г\.$/, '')
