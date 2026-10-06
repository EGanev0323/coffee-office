import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // При локална разработка заявките към /api отиват към Spring Boot
    proxy: { '/api': 'http://localhost:8080' }
  }
})
