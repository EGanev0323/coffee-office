import { useAuthStore } from './stores/auth'

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.status = status
  }
}

/** Малка обвивка около fetch: добавя токена и превръща грешките в четими съобщения. */
export async function api(path, { method = 'GET', body } = {}) {
  const auth = useAuthStore()
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth.token) headers.Authorization = `Bearer ${auth.token}`

  let res
  try {
    res = await fetch(`/api${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined
    })
  } catch {
    throw new ApiError('Сървърът не отговаря. Провери връзката и опитай отново.', 0)
  }

  if (res.status === 401 && auth.token && path !== '/auth/login') {
    auth.logout()
    window.location.assign('/login?expired=1')
    throw new ApiError('Сесията изтече. Влез отново.', 401)
  }
  if (res.status === 204) return null

  const text = await res.text()
  let data = null
  if (text) {
    try { data = JSON.parse(text) } catch { data = null }
  }
  if (!res.ok) {
    throw new ApiError(data?.message || `Заявката не успя (код ${res.status}).`, res.status)
  }
  return data
}
