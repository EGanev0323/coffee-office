import { reactive } from 'vue'

export const toasts = reactive([])
let seq = 0

export function notify(message, type = 'ok') {
  const toast = { id: ++seq, message, type }
  toasts.push(toast)
  setTimeout(() => {
    const i = toasts.findIndex((t) => t.id === toast.id)
    if (i >= 0) toasts.splice(i, 1)
  }, type === 'error' ? 6000 : 3500)
}
