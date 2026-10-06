<template>
  <div class="admin">
    <nav class="subnav" aria-label="Админ секции">
      <RouterLink :to="{ name: 'admin' }" exact-active-class="is-active" active-class="">Месечен отчет</RouterLink>
      <RouterLink :to="{ name: 'admin-users' }">Колеги</RouterLink>
      <RouterLink :to="{ name: 'admin-packages' }">Пакети</RouterLink>
      <RouterLink :to="{ name: 'admin-qr' }">QR код</RouterLink>
    </nav>
    <RouterView />
  </div>
</template>

<style scoped>
.admin { display: grid; gap: 1.5rem; }
.subnav { display: flex; gap: 1.5rem; border-bottom: 1px solid var(--rule); flex-wrap: wrap; }
.subnav a {
  color: var(--muted);
  text-decoration: none;
  font-weight: 600;
  font-size: var(--step--1);
  padding: 0.6rem 0;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}
.subnav a:hover { color: var(--ink); }
.subnav a.is-active { color: var(--ink); border-bottom-color: var(--marker); }
</style>
