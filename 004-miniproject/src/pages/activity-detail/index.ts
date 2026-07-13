import type { Activity } from '../../types/api'
import { formatDateTime, statusLabel } from '../../utils/format'
import { ensureVerified } from '../../utils/navigation'
import { request, showError } from '../../utils/request'
import { session } from '../../utils/session'

type Detail = Activity & {
  startAtText: string
  endAtText: string
  applyDeadlineText: string
  lifecycleText: string
  lifecycleTone: string
  reviewText: string
  modeText: string
  actionHint: string
}

const lifecycleTones: Record<string, string> = {
  RECRUITING: 'status--success',
  IN_PROGRESS: 'status--success',
  COMPLETION_PENDING: 'status--warn',
  CANCELLED: 'status--danger',
  EXPIRED: 'status--danger',
}

Page({
  data: {
    id: '',
    state: 'loading',
    detail: null as Detail | null,
    imageUrls: [] as string[],
    isCreator: false,
    favoriteBusy: false,
  },

  onLoad(options: Record<string, string | undefined>) {
    if (!ensureVerified()) return
    if (!options.id) { this.setData({ state: 'not-found' }); return }
    this.setData({ id: options.id })
    void this.load()
  },

  async load() {
    this.setData({ state: 'loading' })
    try {
      const detail = await request<Activity>({ path: `/activities/${this.data.id}` })
      const view: Detail = {
        ...detail,
        startAtText: formatDateTime(detail.startAt),
        endAtText: formatDateTime(detail.endAt),
        applyDeadlineText: formatDateTime(detail.applyDeadline),
        lifecycleText: statusLabel(detail.lifecycleStatus),
        lifecycleTone: lifecycleTones[detail.lifecycleStatus] || '',
        reviewText: statusLabel(detail.reviewStatus),
        modeText: detail.meetingMode === 'ONLINE' ? '线上' : detail.meetingMode === 'OFFLINE' ? '线下' : '线上 + 线下',
        actionHint: detail.lifecycleStatus === 'RECRUITING'
          ? '正在招募中，申请通过后可查看成员地点并进入活动会话。'
          : '当前活动不在招募状态，仍可查看已公开信息。',
      }
      const urls = await Promise.all(detail.mediaIds.map(async (id) => {
        try { return (await request<{ url: string }>({ path: `/files/${id}/url` })).url } catch { return '' }
      }))
      this.setData({ detail: view, imageUrls: urls.filter(Boolean), isCreator: detail.creatorId === session.user?.id, state: 'ready' })
    } catch (error) {
      this.setData({ state: 'error' })
      showError(error)
    }
  },

  apply() { wx.navigateTo({ url: `/packageBusiness/application/index?activityId=${this.data.id}` }) },
  manage() { wx.navigateTo({ url: `/packageBusiness/application-manage/index?activityId=${this.data.id}` }) },
  edit() { wx.navigateTo({ url: `/packageBusiness/activity-editor/index?id=${this.data.id}` }) },

  async favorite() {
    this.setData({ favoriteBusy: true })
    try {
      await request<void>({ path: `/activities/${this.data.id}/favorite`, method: 'POST', data: {} })
      wx.showToast({ title: '已收藏', icon: 'success' })
    } catch (error) { showError(error) }
    finally { this.setData({ favoriteBusy: false }) }
  },

  report() {
    wx.navigateTo({ url: `/packageAccount/safety/index?targetType=ACTIVITY&targetId=${this.data.id}` })
  },
})
