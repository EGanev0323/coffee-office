const moneyFmt = new Intl.NumberFormat('bg-BG', { style: 'currency', currency: 'EUR' })
const dateTimeFmt = new Intl.DateTimeFormat('bg-BG', {
  day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
})
const monthFmt = new Intl.DateTimeFormat('bg-BG', { month: 'long', year: 'numeric' })

export const money = (v) => moneyFmt.format(Number(v ?? 0))
export const dateTime = (v) => dateTimeFmt.format(new Date(v))
export const coffees = (n) => (n === 1 ? '1 кафе' : `${n} кафета`)
export const monthLabel = (year, month) => monthFmt.format(new Date(year, month - 1, 1)).replace(/\s*г\.$/, '')
