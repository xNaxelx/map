import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  // Укажите здесь имя вашего репозитория на GitHub
  base: '/map/', // <-- ДОБАВИТЬ ЭТУ СТРОКУ
  plugins: [react()],
})