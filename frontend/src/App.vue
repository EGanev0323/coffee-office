<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import ConfirmDialog from './components/ConfirmDialog.vue'
import ToastStack from './components/ToastStack.vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const showNav = computed(() => auth.isLoggedIn && auth.user && !route.meta.public)

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header v-if="showNav" class="topbar">
    <RouterLink to="/" class="brand">Кафе дъската</RouterLink>
    <nav class="mainnav" aria-label="Основна навигация">
      <RouterLink to="/" exact-active-class="is-active" active-class="">Моите кафета</RouterLink>
      <RouterLink v-if="auth.isAdmin" to="/admin">Админ</RouterLink>
      <RouterLink to="/account">Профил</RouterLink>
      <button type="button" class="linklike" @click="logout">Изход</button>
    </nav>
  </header>
  <main class="page">
    <RouterView />
  </main>
  <ConfirmDialog />
  <ToastStack />
</template>
