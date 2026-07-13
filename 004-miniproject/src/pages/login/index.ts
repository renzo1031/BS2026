import { request, showError } from '../../utils/request'
import { routeAfterLogin } from '../../utils/navigation'
import { session } from '../../utils/session'
import type { Tokens } from '../../types/api'

const DEVELOPMENT_LOGIN_CODE = 'student-a'

Page({
  data: {
    agreed: false,
    loading: false,
  },

  onLoad() {
    if (session.authenticated) routeAfterLogin()
  },

  onAgreementChange(event: WechatMiniprogram.CheckboxGroupChange) {
    this.setData({ agreed: event.detail.value.includes('agree') })
  },

  async login() {
    if (!this.data.agreed) {
      wx.showToast({ title: '请先阅读并同意隐私说明', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      let developmentCode = ''
      try {
        if (wx.getAccountInfoSync().miniProgram.envVersion === 'develop') developmentCode = DEVELOPMENT_LOGIN_CODE
      } catch {
        // Runtime metadata unavailable: use the normal WeChat login flow.
      }
      const code = developmentCode || await new Promise<string>((resolve, reject) => {
        wx.login({
          success: (result) => result.code ? resolve(result.code) : reject(new Error('微信登录凭证为空')),
          fail: reject,
        })
      })
      const tokens = await request<Tokens>({
        path: '/auth/wechat-login',
        method: 'POST',
        retry: false,
        data: { code },
      })
      session.save(tokens)
      routeAfterLogin()
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ loading: false })
    }
  },
})
