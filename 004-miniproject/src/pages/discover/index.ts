import type { Activity, PageResult } from '../../types/api'
import { formatDateTime, queryString } from '../../utils/format'
import { ensureVerified } from '../../utils/navigation'
import { request, showError } from '../../utils/request'

type ActivityCard = Activity & {
  startAtText: string
  modeText: string
  seatText: string
  badge: string
  badgeTone: string
}

Page({
  data: {
    state: 'loading',
    records: [] as ActivityCard[],
    total: '0',
    keyword: '',
    modeIndex: 0,
    modeLabels: ['全部形式', '线下', '线上', '混合'],
    modeValues: ['', 'OFFLINE', 'ONLINE', 'HYBRID'],
    page: 1,
    hasMore: true,
    loadingMore: false,
  },

  onShow() {
    if (ensureVerified() && this.data.state === 'loading') void this.load(true)
  },

  onPullDownRefresh() {
    void this.load(true).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loadingMore) void this.load(false)
  },

  onKeywordInput(event: WechatMiniprogram.Input) {
    this.setData({ keyword: event.detail.value })
  },

  selectMode(event: WechatMiniprogram.BaseEvent) {
    const modeIndex = Number(event.currentTarget.dataset.index)
    if (!Number.isInteger(modeIndex) || modeIndex < 0 || modeIndex >= this.data.modeValues.length) return
    this.setData({ modeIndex })
    void this.load(true)
  },

  search() { void this.load(true) },

  async load(reset: boolean) {
    if (reset) this.setData({ state: 'loading', page: 1, hasMore: true })
    else this.setData({ loadingMore: true })
    try {
      const page = reset ? 1 : this.data.page
      const mode = this.data.modeValues[this.data.modeIndex]
      const result = await request<PageResult<Activity>>({
        path: `/activities${queryString({ page, size: 10, keyword: this.data.keyword.trim(), mode })}`,
      })
      const next = result.records.map((item) => ({
        ...item,
        startAtText: formatDateTime(item.startAt),
        modeText: item.meetingMode === 'ONLINE' ? '线上' : item.meetingMode === 'OFFLINE' ? '线下' : '混合',
        seatText: item.acceptedCount >= item.capacity ? `${item.acceptedCount}/${item.capacity} 人` : `还可加入 ${Math.max(0, item.capacity - item.acceptedCount)} 人`,
        badge: item.acceptedCount >= item.capacity ? '已满员' : item.acceptedCount === 0 ? '可加入' : '招募中',
        badgeTone: item.acceptedCount >= item.capacity ? 'full' : 'hot',
      }))
      const records = reset ? next : [...this.data.records, ...next]
      this.setData({
        records,
        total: String(result.total),
        state: records.length ? 'ready' : 'empty',
        page: page + 1,
        hasMore: records.length < Number(result.total),
      })
    } catch (error) {
      if (reset) this.setData({ state: 'error' })
      showError(error)
    } finally {
      this.setData({ loadingMore: false })
    }
  },

  openDetail(event: WechatMiniprogram.BaseEvent) {
    wx.navigateTo({ url: `/pages/activity-detail/index?id=${event.currentTarget.dataset.id}` })
  },

  createActivity() {
    wx.navigateTo({ url: '/packageBusiness/activity-editor/index' })
  },
})
