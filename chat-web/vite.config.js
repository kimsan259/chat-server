// chat-web/vite.config.js
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  server: {
    proxy: {
      '/api': { target: 'http://localhost:8083', changeOrigin: true },
      '/ws-handler': { target: 'http://localhost:8083', ws: true, changeOrigin: true }
    }
  }
});

