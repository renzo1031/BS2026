import type { Activity, ApplicationView, PageResult } from '../../types/api'
import { formatDateTime, statusLabel } from '../../utils/format'
import { ensureVerified } from '../../utils/navigation'
import { request, showError } from '../../utils/request'

type ApplicationRow = ApplicationView & { createdAtText: string; statusText: string }
type ManageActivity = Activity & { reviewText: string; lifecycleText: string }

type ManageStats = {
  total: number
  pending: number
  accepted: number
  rejected: number
  openSeats: number
}

Page({
  data: {
    activityId: '', state: 'loading', activity: null as ManageActivity | null,
    applications: [] as ApplicationRow[], actionBusy: false,
    stats: null as ManageStats | null,
  },
  onLoad(options: Record<string, string | undefined>) {
    if (!ensureVerified()) return
    if (!options.activityId) { this.setData({ state: 'not-found' }); return }
    this.setData({ activityId: options.activityId }); void this.load()
  },
  onPullDownRefresh() { void this.load().finally(() => wx.stopPullDownRefresh()) },
  async load() {
    this.setData({ state: 'loading' })
    try {
      const [activity, applications] = await Promise.all([
        request<Activity>({ path: `/activities/${this.data.activityId}` }),
        request<PageResult<ApplicationView>>({ path: `/activities/${this.data.activityId}/applications?page=1&size=50` }),
      ])
      const rows = applications.records.map((item) => ({ ...item, createdAtText: formatDateTime(item.createdAt), statusText: statusLabel(item.status) }))
      this.setData({
        activity: {
          ...activity,
          reviewText: activity.reviewStatus === 'NOT_SUBMITTED' ? '未提交' : statusLabel(activity.reviewStatus),
          lifecycleText: statusLabel(activity.lifecycleStatus),
        },
        applications: rows,
        stats: {
          total: rows.length,
          pending: rows.filter((item) => item.status === 'PENDING').length,
          accepted: rows.filter((item) => item.status === 'ACCEPTED').length,
          rejected: rows.filter((item) => item.status === 'REJECTED').length,
          openSeats: Math.max(0, activity.capacity - activity.acceptedCount),
        },
        state: 'ready',
      })
    } catch (error) { this.setData({ state: 'error' }); showError(error) }
  },
  async decide(event: WechatMiniprogram.BaseEvent) {
    if (this.data.actionBusy) return
    const id = String(event.currentTarget.dataset.id)
    const accept = event.currentTarget.dataset.accept === true || event.currentTarget.dataset.accept === 'true'
    const item = this.data.applications.find((candidate) => candidate.id === id)
    if (!item) return
    this.setData({ actionBusy: true })
    try {
      const modal = await wx.showModal({ title: accept ? '接受申请' : '拒绝申请', content: accept ? `确认接受 ${item.applicantNickname} 加入活动？` : `确认拒绝 ${item.applicantNickname} 的申请？`, editable: !accept, placeholderText: '可填写拒绝原因' })
      if (!modal.confirm) return
      await request<ApplicationView>({ path: `/applications/${id}/decision`, method: 'POST', data: { version: item.version, accept, reason: modal.content || null } })
      wx.showToast({ title: accept ? '已接受' : '已拒绝', icon: 'success' }); await this.load()
    } catch (error) { showError(error) }
    finally { this.setData({ actionBusy: false }) }
  },
  async lifecycle(event: WechatMiniprogram.BaseEvent) {
    if (this.data.actionBusy) return
    const action = String(event.currentTarget.dataset.action)
    const activity = this.data.activity
    if (!activity) return
    let path = action
    let data: WechatMiniprogram.IAnyObject = { version: activity.version }
    this.setData({ actionBusy: true })
    try {
      if (action === 'cancel') {
        const modal = await wx.showModal({ title: '取消活动', editable: true, placeholderText: '请说明取消原因', content: '取消后会通知已加入成员，且不可恢复。' })
        if (!modal.confirm) return
        data = { version: activity.version, reason: modal.content || '发起人取消活动' }
      }
      await request<Activity>({ path: `/activities/${activity.id}/${path}`, method: 'POST', data })
      wx.showToast({ title: '活动状态已更新', icon: 'success' }); await this.load()
    } catch (error) { showError(error) }
    finally { this.setData({ actionBusy: false }) }
  },
  edit() { wx.navigateTo({ url: `/packageBusiness/activity-editor/index?id=${this.data.activityId}` }) },
})
