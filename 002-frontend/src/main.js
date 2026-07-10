import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/http'
import { useAuthStore } from './store/auth'
import './style.css'
import 'vfonts/Lato.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia).use(router)

setUnauthorizedHandler(() => {
  const auth = useAuthStore(pinia)
  const current = router.currentRoute.value
  auth.clearSession()
  if (current.name !== 'login') {
    void router.replace({ name: 'login', query: { redirect: current.fullPath } })
  }
})

app.mount('#app')
