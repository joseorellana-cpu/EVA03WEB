import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
// - En dev: proxy /api → Gateway (8080). Sin CORS. Nunca se llama a 8081/8082/8083.
// - En build: sin source maps (menor superficie de ataque y menos datos expuestos).
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    sourcemap: false,
  },
  server: {
    port: 5173,
    // Proxy sólo en desarrollo; en producción se asume mismo origen
    // detrás de nginx (proxy inverso /api → Gateway).
    proxy: {
      '/api': {
        target: process.env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
