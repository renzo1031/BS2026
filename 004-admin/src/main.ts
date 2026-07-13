import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import 'element-plus/theme-chalk/el-loading.css'
import 'element-plus/theme-chalk/el-overlay.css'
import App from './App.vue'
import router from './router'
import { permissionDirective } from './directives/permission'
import './styles/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.directive('permission', permissionDirective)
app.mount('#app')
