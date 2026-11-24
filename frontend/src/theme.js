/*
  Избор на тема: „system“ (по системната настройка), „light“ или „dark“.
  Изборът се пази в localStorage и слага data-theme на <html>; CSS-ът в styles.css
  прилага тъмните токени при data-theme="dark" или при системна тъмна тема без изрично „светла“.
  Обновява и meta[name=theme-color], за да съвпада лентата на браузъра с фона.
*/
import { ref } from 'vue'

const KEY = 'coffee-theme'
const VALID = ['system', 'light', 'dark']
const darkQuery = typeof window !== 'undefined' ? window.matchMedia('(prefers-color-scheme: dark)') : null

/** Текущият избор (реактивен) – споделя се между превключвателя и приложението. */
export const theme = ref(read())

function read() {
  try {
    const v = localStorage.getItem(KEY)
    return VALID.includes(v) ? v : 'system'
  } catch {
    return 'system'
  }
}

/** Коя тема реално се вижда при даден избор. */
function resolvedDark(choice) {
  if (choice === 'dark') return true
  if (choice === 'light') return false
  return !!darkQuery?.matches
}

function apply(choice) {
  const root = document.documentElement
  if (choice === 'system') root.removeAttribute('data-theme')
  else root.setAttribute('data-theme', choice)

  // лентата на браузъра (status bar) да съвпада с фона
  let meta = document.querySelector('meta[name="theme-color"]:not([media])')
  if (!meta) {
    meta = document.createElement('meta')
    meta.setAttribute('name', 'theme-color')
    document.head.appendChild(meta)
  }
  meta.setAttribute('content', resolvedDark(choice) ? '#121619' : '#eceeec')
}

/** Сменя темата и я запазва. */
export function setTheme(choice) {
  if (!VALID.includes(choice)) choice = 'system'
  theme.value = choice
  try { localStorage.setItem(KEY, choice) } catch { /* частен режим */ }
  apply(choice)
}

/** Прилага запазения избор при стартиране. */
export function initTheme() {
  apply(theme.value)
  // при „системна“ – следвай смяната на системната тема на живо
  darkQuery?.addEventListener?.('change', () => { if (theme.value === 'system') apply('system') })
}
