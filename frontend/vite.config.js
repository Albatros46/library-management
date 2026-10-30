import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // All API calls go through the gateway, so the browser never talks to services directly.
    proxy: { '/api': 'http://localhost:8080' },
  },
});
