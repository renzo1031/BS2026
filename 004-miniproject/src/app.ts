import { session } from './utils/session'

App({
  globalData: {
    launchedAt: Date.now(),
  },
  onLaunch() {
    session.restore()
  },
  onError(error) {
    console.error('miniprogram runtime error', error)
  },
})
