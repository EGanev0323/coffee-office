import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// По подразбиране /api отива към локалния Spring Boot.
// С COFFEE_API може да се насочи към работещ сървър, напр. COFFEE_API=http://100.64.0.20:8090
const apiTarget = process.env.COFFEE_API || 'http://localhost:8080'
if (process.env.COFFEE_API) console.log(`  /api → ${apiTarget}`)

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: apiTarget, changeOrigin: true, secure: true }
    }
  }
})
