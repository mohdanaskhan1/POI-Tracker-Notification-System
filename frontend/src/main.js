import { createApp } from 'vue'
import App from './App.vue'
import axios from 'axios'

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js')
}

axios.defaults.withCredentials = true

axios.interceptors.response.use(
  (res) => res,
  (err) => {
    console.error('API error', err)
    return Promise.reject(err)
  }
)

createApp(App).mount('#app')
