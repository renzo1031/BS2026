import type { Overview } from '../../types/api'
import { ensureAuthenticated } from '../../utils/navigation'
import { request, showError } from '../../utils/request'
import { session } from '../../utils/session'

type QuickAction = {
  title: string
  desc: string
  url: string
  icon: string
}

function parseInterestTags(value?: string | null) {
  if (!value) return [] as string[]
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.map((item) => String(item).trim()).filter(Boolean) : []
  } catch {
    return []
  }
}

function calcCompletion(overview: Overview) {
  const user = overview.user
  const checkpoints = [
    Boolean(user.nickname?.trim()),
    Boolean(user.avatarFileId),
    Boolean(user.gradeName?.trim()),
    Boolean(user.majorName?.trim()),
    Boolean(user.bio?.trim()),
    parseInterestTags(user.interestTagsJson).length > 0,
    Boolean(user.campusId),
    user.verificationStatus === 'APPROVED',
  ]
  return Math.round((checkpoints.filter(Boolean).length / checkpoints.length) * 100)
}

Page({
  data: {
    state: 'loading',
    overview: null as Overview | null,
    avatarUrl: '',
    interestTags: [] as string[],
    completionPercent: 0,
    completionText: '待补全',
    verificationHint: '',
    quickActions: [
      { title: '编辑资料', desc: '完善头像、年级、专业和简介', url: '/packageAccount/profile-edit/index', icon: '/assets/icons/action-edit.png' },
      { title: '收藏与信誉', desc: '查看收藏、评分与评价记录', url: '/packageAccount/favorites-reputation/index', icon: '/assets/icons/action-star.png' },
      { title: '认证与隐私', desc: '查看身份绑定与安全状态', url: '/packageAccount/security-privacy/index', icon: '/assets/icons/action-shield.png' },
      { title: '举报与申诉', desc: '处理异常搭子、黑名单和申诉', url: '/packageAccount/safety/index', icon: '/assets/icons/action-flag.png' },
    ] as QuickAction[],
    loggingOut: false,
  },

  onShow() { if (ensureAuthenticated()) void this.load() },
  onPullDownRefresh() { void this.load().finally(() => wx.stopPullDownRefresh()) },

  onAvatarImageError() { this.setData({ avatarUrl: '' }) },

  async load() {
    this.setData({ state: 'loading' })
    try {
      const overview = await request<Overview>({ path: '/me/overview' })
      session.updateUser(overview.user)
      const interestTags = parseInterestTags(overview.user.interestTagsJson)
      const completionPercent = calcCompletion(overview)
      let avatarUrl = ''
      if (overview.user.avatarFileId) {
        try { avatarUrl = (await request<{ url: string }>({ path: `/files/${overview.user.avatarFileId}/url` })).url } catch { /* 回退到首字头像。 */ }
      }
      this.setData({ overview, avatarUrl, state: 'ready' })
      this.setData({
        interestTags,
        completionPercent,
        completionText: completionPercent >= 85 ? '资料很完整' : completionPercent >= 60 ? '资料可用' : '还可以继续完善',
        verificationHint: overview.user.verificationStatus === 'APPROVED'
          ? '已完成校园认证，可以直接发布和加入活动'
          : '完成校园认证后，发布、报名与消息能力会全部解锁',
      })
    } catch (error) {
      this.setData({ state: 'error' })
      showError(error)
    }
  },

  open(event: WechatMiniprogram.BaseEvent) {
    const url = String(event.currentTarget.dataset.url)
    if (['/pages/discover/index', '/pages/messages/index', '/pages/profile/index'].includes(url)) {
      wx.switchTab({ url })
    } else {
      wx.navigateTo({ url })
    }
  },

  async logout() {
    this.setData({ loggingOut: true })
    try {
      await request<void>({ path: '/auth/logout', method: 'POST', data: { refreshToken: session.refreshToken }, retry: false })
    } catch { /* 本地登录态仍需清除。 */ }
    finally {
      session.clear()
      this.setData({ loggingOut: false })
      wx.reLaunch({ url: '/pages/login/index' })
    }
  },
})
