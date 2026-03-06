import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    lib: {
      entry: 'src/index.js',
      name: 'FamilyTree',
      fileName: 'familytree',
      formats: ['iife', 'es']
    },
    rollupOptions: {
      output: {
        extend: true
      }
    }
  }
});
