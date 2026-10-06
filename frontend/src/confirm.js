import { reactive } from 'vue'

export const confirmState = reactive({
  open: false, title: '', message: '', confirmText: 'Потвърди', danger: false, resolve: null
})

/** Показва диалог за потвърждение и връща Promise<boolean>. */
export function confirmAction(options) {
  return new Promise((resolve) => {
    Object.assign(confirmState, {
      title: '', message: '', confirmText: 'Потвърди', danger: false,
      ...options,
      open: true,
      resolve
    })
  })
}
