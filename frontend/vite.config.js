import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          charts: ['recharts'],
          dnd: ['@hello-pangea/dnd'],
        },
      },
    },
  },
  server: {
    port: 5173,
    // In development, forward API calls to the Spring Boot backend (avoids CORS)
    proxy: { '/api': 'http://localhost:8080' },
  },
});
