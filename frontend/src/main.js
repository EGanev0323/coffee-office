import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { installMotionDirectives } from './motion/directives'
import '@fontsource/onest/400.css'
import '@fontsource/onest/500.css'
import '@fontsource/onest/600.css'
import '@fontsource/onest/700.css'
import '@fontsource/caveat/700.css'
import './styles.css'

const app = createApp(App).use(createPinia()).use(router)
installMotionDirectives(app)
app.mount('#app')
