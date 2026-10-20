<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { confirmAction } from '../confirm'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'
import { notify } from '../notify'
import SplitText from '../components/fx/SplitText.vue'
import { shake } from '../motion'
import { burst } from '../motion/burst'

const auth = useAuthStore()
const router = useRouter()

async function logout() {
  const ok = await confirmAction({
    title: 'Изход',
    message: 'Ще трябва да влезеш отново с потребителското име и паролата.',
    confirmText: 'Излез',
    danger: true
  })
  if (!ok) return
  auth.logout()
  router.push({ name: 'login' })
}
const form = reactive({ currentPassword: '', newPassword: '', repeat: '' })
const error = ref('')
const busy = ref(false)
const formEl = ref(null)
const submitBtn = ref(null)

async function submit() {
  error.value = ''
  if (form.newPassword !== form.repeat) {
    error.value = 'Новата парола и повторението не съвпадат.'
    shake(formEl.value)
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
    burst(submitBtn.value, { kind: 'strokes', count: 12 })
  } catch (e) {
    error.value = e.message
    shake(formEl.value)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="account">
    <SplitText tag="h1" text="Профил" :stagger="0.05" />
    <p v-reveal="{ delay: 0.15, y: 10 }" class="hint">{{ auth.user?.displayName }}, потребителско име {{ auth.user?.username }}</p>

    <form ref="formEl" v-spotlight v-reveal="{ delay: 0.2 }" class="panel stack" @submit.prevent="submit">
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
      <button ref="submitBtn" v-magnet="{ strength: 0.15, padding: 16 }" class="btn btn-primary btn-block-phone" :disabled="busy">Смени паролата</button>
    </form>

    <section v-reveal="{ delay: 0.3 }" class="panel logout">
      <div>
        <h2>Изход</h2>
        <p class="hint">Излизаш само от това устройство.</p>
      </div>
      <button type="button" class="btn btn-danger" @click="logout">Излез</button>
    </section>
  </div>
</template>

<style scoped>
.account { max-width: 480px; display: grid; gap: 1rem; }
.logout { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
</style>
