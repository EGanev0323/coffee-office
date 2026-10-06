import { useAuthStore } from './stores/auth'

/*
  Абонамент за събитията на дъската (Server-Sent Events).
  Ползваме fetch вместо EventSource, за да изпратим токена в Authorization хедъра,
  а не в адреса. При прекъсване се свързва наново с нарастваща пауза.
*/
export function subscribeBoardEvents(handlers) {
  const auth = useAuthStore()
  let controller = null
  let stopped = false
  let retryMs = 1000

  async function connect() {
    if (stopped || !auth.token) return
    controller = new AbortController()
    try {
      const res = await fetch('/api/board/events', {
        headers: { Authorization: `Bearer ${auth.token}`, Accept: 'text/event-stream' },
        cache: 'no-store',
        signal: controller.signal
      })
      if (res.status === 401 || res.status === 403) return // сесията е изтекла – api() ще прати към вход
      if (!res.ok || !res.body) throw new Error(`stream ${res.status}`)

      retryMs = 1000
      handlers.open?.()

      const reader = res.body.pipeThrough(new TextDecoderStream()).getReader()
      let buffer = ''
      for (;;) {
        const { value, done } = await reader.read()
        if (done) break
        buffer += value.replace(/\r/g, '')
        let end
        while ((end = buffer.indexOf('\n\n')) >= 0) {
          dispatch(buffer.slice(0, end))
          buffer = buffer.slice(end + 2)
        }
      }
    } catch {
      if (stopped) return
    }
    if (!stopped) {
      setTimeout(connect, retryMs)
      retryMs = Math.min(retryMs * 2, 30000)
    }
  }

  function dispatch(raw) {
    let event = 'message'
    let data = ''
    for (const line of raw.split('\n')) {
      if (line.startsWith(':')) continue // коментар / ping
      if (line.startsWith('event:')) event = line.slice(6).trim()
      else if (line.startsWith('data:')) data += line.slice(5).trimStart()
    }
    if (!data) return
    try {
      handlers[event]?.(JSON.parse(data))
    } catch {
      // повредено съобщение – пропускаме го
    }
  }

  connect()
  return () => {
    stopped = true
    controller?.abort()
  }
}