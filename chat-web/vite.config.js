// vite.config.js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// chat-web/vite.config.js
export default defineConfig({
  server: {
    proxy: {
      '/api': { target: 'http://127.0.0.1:8083', changeOrigin: true },
      '/ws-handler': { target: 'http://127.0.0.1:8083', ws: true, changeOrigin: true },
    },
  },
});


