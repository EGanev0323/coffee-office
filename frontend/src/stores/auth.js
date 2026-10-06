import { defineStore } from 'pinia'
import { api } from '../api'

const TOKEN_KEY = 'office-coffee.token'

/*
  „Запомни ме“ → токенът е в localStorage и остава след затваряне на браузъра.
  Без отметка → в sessionStorage и изчезва със затварянето на браузъра.
*/
function safe(fn, fallback = null) {
  try { return fn() } catch { return fallback }
}
function readToken() {
  return safe(() => localStorage.getItem(TOKEN_KEY)) || safe(() => sessionStorage.getItem(TOKEN_KEY))
}
function clearToken() {
  safe(() => localStorage.removeItem(TOKEN_KEY))
  safe(() => sessionStorage.removeItem(TOKEN_KEY))
}
function saveToken(token, remember) {
  clearToken()
  safe(() => (remember ? localStorage : sessionStorage).setItem(TOKEN_KEY, token))
}
function isRemembered() {
  return !!safe(() => localStorage.getItem(TOKEN_KEY))
}

export const useAuthStore = defineStore('auth', {
  state: () => ({ token: readToken(), user: null }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    isAdmin: (s) => s.user?.role === 'ADMIN'
  },
  actions: {
    async login(username, password, remember) {
      const res = await api('/auth/login', { method: 'POST', body: { username, password, remember } })
      this.token = res.token
      this.user = res.user
      saveToken(res.token, remember)
    },
    /** Подменя токена (напр. след смяна на парола), като запазва избора за „Запомни ме“. */
    replaceToken(token) {
      saveToken(token, isRemembered())
      this.token = token
    },
    async fetchMe() {
      this.user = await api('/me')
    },
    logout() {
      this.token = null
      this.user = null
      clearToken()
    }
  }
})