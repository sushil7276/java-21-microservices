import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development every /api call is proxied to the Spring Cloud Gateway,
// so the browser never talks to another origin (no CORS setup needed).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: process.env.VITE_GATEWAY_URL || 'http://localhost:8080', changeOrigin: true },
    },
  },
});
