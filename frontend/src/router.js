import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from './stores/auth'
import LoginView from './views/LoginView.vue'
import HomeView from './views/HomeView.vue'
import AccountView from './views/AccountView.vue'
import AdminLayout from './views/admin/AdminLayout.vue'
import AdminOverview from './views/admin/AdminOverview.vue'
import AdminUsers from './views/admin/AdminUsers.vue'
import AdminPackages from './views/admin/AdminPackages.vue'
import DrinkView from './views/DrinkView.vue'
import AdminQr from './views/admin/AdminQr.vue'

const router = createRouter({
  history: createWebHistory(),
  linkActiveClass: 'is-active',
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/', name: 'home', component: HomeView },
    { path: '/account', name: 'account', component: AccountView },
    { path: '/drink', name: 'drink', component: DrinkView }, // отваря се от QR кода на кафемашината
    {
      path: '/admin',
      component: AdminLayout,
      meta: { admin: true },
      children: [
        { path: '', name: 'admin', component: AdminOverview },
        { path: 'users', name: 'admin-users', component: AdminUsers },
        { path: 'packages', name: 'admin-packages', component: AdminPackages },
        { path: 'packages', name: 'admin-packages', component: AdminPackages },
        { path: 'qr', name: 'admin-qr', component: AdminQr }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    return auth.isLoggedIn ? { name: 'home' } : true
  }
  if (!auth.isLoggedIn) {
    return { name: 'login', query: to.fullPath !== '/' ? { next: to.fullPath } : {} }
  }
  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      auth.logout()
      return { name: 'login' }
    }
  }
  if (to.matched.some((r) => r.meta.admin) && !auth.isAdmin) {
    return { name: 'home' }
  }
  return true
})

export default router
