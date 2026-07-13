import type { Activity, ApplicationView, PageResult } from '../../types/api'
import { formatDateTime, statusLabel } from '../../utils/format'
import { ensureVerified } from '../../utils/navigation'
import { request, showError } from '../../utils/request'

type ActivityRow = Activity & { timeText: string; statusText: string; reviewText: string; lifecycleText: string }
type ApplicationRow = ApplicationView & { timeText: string; statusText: string; statusTone: string }

type ActivityStats = {
  total: number
  published: number
  draft: number
  recruiting: number
  pending: number
}

Page({
  data: {
    tab: 'published', state: 'loading',
    activities: [] as ActivityRow[], applications: [] as ApplicationRow[],
    stats: null as ActivityStats | null,
  },
  onShow() { if (ensureVerified()) void this.load() },
  onPullDownRefresh() { void this.load().finally(() => wx.stopPullDownRefresh()) },
  switchTab(event: WechatMiniprogram.BaseEvent) { this.setData({ tab: event.currentTarget.dataset.tab }) },
  async load() {
    this.setData({ state: 'loading' })
    try {
      const [activities, applications] = await Promise.all([
        request<PageResult<Activity>>({ path: '/activities/mine?page=1&size=50' }),
        request<PageResult<ApplicationView>>({ path: '/applications/mine?page=1&size=50' }),
      ])
      this.setData({
        activities: activities.records.map((item) => ({
          ...item,
          timeText: formatDateTime(item.startAt),
          statusText: statusLabel(item.lifecycleStatus),
          reviewText: statusLabel(item.reviewStatus),
          lifecycleText: statusLabel(item.lifecycleStatus),
        })),
        applications: applications.records.map((item) => ({
          ...item,
          timeText: formatDateTime(item.createdAt),
          statusText: statusLabel(item.status),
          statusTone: item.status === 'PENDING' ? 'status--warn' : item.status === 'ACCEPTED' ? 'status--success' : 'status--danger',
        })),
        stats: {
          total: activities.records.length + applications.records.length,
          published: activities.records.length,
          draft: activities.records.filter((item) => item.lifecycleStatus === 'DRAFT').length,
          recruiting: activities.records.filter((item) => item.lifecycleStatus === 'RECRUITING').length,
          pending: applications.records.filter((item) => item.status === 'PENDING').length,
        },
        state: 'ready',
      })
    } catch (error) { this.setData({ state: 'error' }); showError(error) }
  },
  openActivity(event: WechatMiniprogram.BaseEvent) { wx.navigateTo({ url: `/pages/activity-detail/index?id=${event.currentTarget.dataset.id}` }) },
  edit(event: WechatMiniprogram.BaseEvent) { wx.navigateTo({ url: `/packageBusiness/activity-editor/index?id=${event.currentTarget.dataset.id}` }) },
  manage(event: WechatMiniprogram.BaseEvent) { wx.navigateTo({ url: `/packageBusiness/application-manage/index?activityId=${event.currentTarget.dataset.id}` }) },
  async withdraw(event: WechatMiniprogram.BaseEvent) {
    const id = String(event.currentTarget.dataset.id)
    const item = this.data.applications.find((candidate) => candidate.id === id)
    if (!item) return
    const modal = await wx.showModal({ title: '确认撤回', content: item.status === 'ACCEPTED' ? '退出已加入活动会释放名额，且无法恢复。' : '撤回后无法再次申请该活动。' })
    if (!modal.confirm) return
    try {
      await request<ApplicationView>({ path: `/applications/${id}/withdraw`, method: 'POST', data: { version: item.version } })
      wx.showToast({ title: '已撤回', icon: 'success' }); await this.load()
    } catch (error) { showError(error) }
  },
})
