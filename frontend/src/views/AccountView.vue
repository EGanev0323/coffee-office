<script setup>
import { reactive, ref } from 'vue'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'
import { notify } from '../notify'

const auth = useAuthStore()
const form = reactive({ currentPassword: '', newPassword: '', repeat: '' })
const error = ref('')
const busy = ref(false)

async function submit() {
  error.value = ''
  if (form.newPassword !== form.repeat) {
    error.value = 'Новата парола и повторението не съвпадат.'
    return
  }
  busy.value = true
  try {
    const res = await api('/me/password', {
      method: 'POST',
      body: { currentPassword: form.currentPassword, newPassword: form.newPassword }
    })
    auth.replaceToken(res.token)
    auth.user = res.user
    Object.assign(form, { currentPassword: '', newPassword: '', repeat: '' })
    notify('Паролата е сменена. Всички други устройства са отписани.')
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="account">
    <h1>Профил</h1>
    <p class="hint">{{ auth.user?.displayName }}, потребителско име {{ auth.user?.username }}</p>

    <form class="panel stack" @submit.prevent="submit">
      <h2>Смяна на парола</h2>
      <p class="hint">Всички други устройства, на които си влязъл, ще бъдат отписани.</p>
      <div class="field">
        <label for="cur">Текуща парола</label>
        <input id="cur" v-model="form.currentPassword" type="password" autocomplete="current-password" required />
      </div>
      <div class="field">
        <label for="new">Нова парола</label>
        <input id="new" v-model="form.newPassword" type="password" autocomplete="new-password" minlength="6" required />
      </div>
      <div class="field">
        <label for="rep">Повтори новата парола</label>
        <input id="rep" v-model="form.repeat" type="password" autocomplete="new-password" minlength="6" required />
      </div>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button class="btn btn-primary" :disabled="busy">Смени паролата</button>
    </form>
  </div>
</template>

<style scoped>
.account { max-width: 480px; display: grid; gap: 1rem; }
</style>
