import type { IdentityView, UserView } from '../../types/api'
import { ensureAuthenticated } from '../../utils/navigation'
import { request, showError } from '../../utils/request'
import { session } from '../../utils/session'

const labels: Record<string, string> = {
  ACTIVE: '正常', LIMITED: '功能受限', SUSPENDED: '已暂停', CLOSED: '已关闭',
  UNVERIFIED: '未认证', PENDING: '审核中', APPROVED: '已认证', REJECTED: '未通过',
  EXPIRED: '已过期', REVOKED: '已撤销',
}

Page({
  data: {
    state: 'loading',
    user: null as UserView | null,
    binding: null as IdentityView | null,
    securityCards: [] as Array<{ label: string; value: string; note: string }>,
    accountStatusText: '',
    verificationStatusText: '',
    bindingStatusText: '',
    canOpenBinding: false,
    loggingOut: false,
  },

  onLoad() {
    if (ensureAuthenticated()) void this.load()
  },

  onPullDownRefresh() {
    void this.load().finally(() => wx.stopPullDownRefresh())
  },

  async load() {
    if (!ensureAuthenticated()) return
    this.setData({ state: 'loading' })
    try {
      const [user, binding] = await Promise.all([
        request<UserView>({ path: '/me' }),
        request<IdentityView | null>({ path: '/me/identity-bindings/current' }),
      ])
      session.updateUser(user)
      this.setData({
        user,
        binding,
        accountStatusText: labels[user.status] || user.status,
        verificationStatusText: labels[user.verificationStatus] || user.verificationStatus,
        bindingStatusText: binding ? labels[binding.status] || binding.status : '',
        securityCards: [
          { label: '账号状态', value: labels[user.status] || user.status, note: user.status === 'ACTIVE' ? '可正常使用' : '部分能力受限' },
          { label: '校园认证', value: labels[user.verificationStatus] || user.verificationStatus, note: user.verificationStatus === 'APPROVED' ? '已解锁发布与报名' : '需完成认证' },
          { label: '身份绑定', value: binding ? labels[binding.status] || binding.status : '未绑定', note: binding?.identifierMasked || '暂无脱敏标识' },
        ],
        canOpenBinding: !binding || ['REJECTED', 'EXPIRED', 'REVOKED'].includes(binding.status),
        state: 'ready',
      })
    } catch (error) {
      this.setData({ state: 'error' })
      showError(error)
    }
  },

  openBinding() {
    wx.navigateTo({ url: '/pages/campus-bind/index' })
  },

  async logout() {
    this.setData({ loggingOut: true })
    try {
      await request<void>({
        path: '/auth/logout',
        method: 'POST',
        data: { refreshToken: session.refreshToken },
        retry: false,
      })
    } catch {
      // 服务端不可达时，本地登录态仍必须清除。
    } finally {
      session.clear()
      this.setData({ loggingOut: false })
      wx.reLaunch({ url: '/pages/login/index' })
    }
  },
})
