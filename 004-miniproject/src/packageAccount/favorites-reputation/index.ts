import type { EvaluationTarget, EvaluationView, FavoriteView, PageResult, ReputationView } from '../../types/api'
import { formatDateTime, statusLabel } from '../../utils/format'
import { ensureVerified } from '../../utils/navigation'
import { request, showError } from '../../utils/request'

type FavoriteRow = FavoriteView & { startAtText: string; statusText: string }
type EvaluationRow = EvaluationView & { createdAtText: string }
type SummaryCard = { label: string; value: string; note: string }

const pageSize = 10
const decimalId = /^[1-9]\d*$/

function favoriteRow(item: FavoriteView): FavoriteRow {
  return {
    ...item,
    startAtText: formatDateTime(item.startAt),
    statusText: item.available ? '可申请' : `${statusLabel(item.lifecycleStatus)}，当前不可申请`,
  }
}

Page({
  data: {
    tab: 'favorites',
    state: 'loading',
    favorites: [] as FavoriteRow[],
    favoritePage: 1,
    favoriteTotal: 0,
    favoriteHasMore: false,
    loadingMore: false,
    reputation: null as ReputationView | null,
    averageRatingText: '0.0',
    distributionRows: [] as Array<{ rating: number; count: string }>,
    recent: [] as EvaluationRow[],
    summaryCards: [] as SummaryCard[],
    activityId: '',
    evaluationState: 'idle',
    targets: [] as EvaluationTarget[],
    ratingLabels: ['5 星', '4 星', '3 星', '2 星', '1 星'],
    ratingValues: [5, 4, 3, 2, 1],
    ratingIndex: 0,
    ratingValue: 5,
    evaluationTagsText: '',
    privateNote: '',
    loadingTargets: false,
    busyTargetId: '',
  },

  onLoad() {
    if (ensureVerified()) void this.load()
  },

  onPullDownRefresh() {
    void this.load().finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.tab === 'favorites') void this.loadMoreFavorites()
  },

  switchTab(event: WechatMiniprogram.BaseEvent) {
    this.setData({ tab: String(event.currentTarget.dataset.tab) })
  },

  async load() {
    if (!ensureVerified()) return
    this.setData({ state: 'loading' })
    try {
      const [favorites, reputation] = await Promise.all([
        request<PageResult<FavoriteView>>({ path: `/favorites?page=1&size=${pageSize}` }),
        request<ReputationView>({ path: '/me/reputation' }),
      ])
      const total = Number(favorites.total)
      this.setData({
        favorites: favorites.records.map(favoriteRow),
        favoritePage: 1,
        favoriteTotal: total,
        favoriteHasMore: favorites.records.length < total,
        ...this.reputationData(reputation),
        state: 'ready',
      })
    } catch (error) {
      this.setData({ state: 'error' })
      showError(error)
    }
  },

  reputationData(reputation: ReputationView) {
    return {
      reputation,
      averageRatingText: reputation.averageRating.toFixed(1),
      summaryCards: [
        { label: '收到评价', value: reputation.receivedCount, note: '来自活动同伴' },
        { label: '平均评分', value: reputation.averageRating.toFixed(1), note: '结构化统计' },
        { label: '五星占比', value: reputation.distribution['5'] || '0', note: '正向反馈' },
      ],
      distributionRows: [5, 4, 3, 2, 1].map((rating) => ({
        rating,
        count: reputation.distribution[String(rating)] || '0',
      })),
      recent: reputation.recent.map((item) => ({ ...item, createdAtText: formatDateTime(item.createdAt) })),
    }
  },

  async loadMoreFavorites() {
    if (this.data.loadingMore || !this.data.favoriteHasMore) return
    const nextPage = this.data.favoritePage + 1
    this.setData({ loadingMore: true })
    try {
      const result = await request<PageResult<FavoriteView>>({ path: `/favorites?page=${nextPage}&size=${pageSize}` })
      const favorites = [...this.data.favorites, ...result.records.map(favoriteRow)]
      const total = Number(result.total)
      this.setData({
        favorites,
        favoritePage: nextPage,
        favoriteTotal: total,
        favoriteHasMore: favorites.length < total,
      })
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ loadingMore: false })
    }
  },

  openActivity(event: WechatMiniprogram.BaseEvent) {
    wx.navigateTo({ url: `/pages/activity-detail/index?id=${String(event.currentTarget.dataset.id)}` })
  },

  async unfavorite(event: WechatMiniprogram.BaseEvent) {
    const activityId = String(event.currentTarget.dataset.id)
    const modal = await wx.showModal({ title: '取消收藏', content: '确定从收藏列表移除该活动吗？' })
    if (!modal.confirm) return
    try {
      await request<void>({ path: `/activities/${activityId}/favorite`, method: 'DELETE' })
      wx.showToast({ title: '已取消收藏', icon: 'success' })
      await this.load()
    } catch (error) {
      showError(error)
    }
  },

  onInput(event: WechatMiniprogram.Input) {
    this.setData({ [String(event.currentTarget.dataset.field)]: event.detail.value })
  },

  onRatingChange(event: WechatMiniprogram.PickerChange) {
    const ratingIndex = Number(event.detail.value)
    this.setData({ ratingIndex, ratingValue: this.data.ratingValues[ratingIndex] || 5 })
  },

  async loadTargets() {
    const activityId = this.data.activityId.trim()
    if (!decimalId.test(activityId)) {
      wx.showToast({ title: '请输入有效的已完成活动 ID', icon: 'none' })
      return
    }
    this.setData({ activityId, loadingTargets: true, evaluationState: 'loading', targets: [] })
    try {
      const targets = await request<EvaluationTarget[]>({ path: `/activities/${activityId}/evaluation-targets` })
      this.setData({ targets, evaluationState: targets.length ? 'ready' : 'empty' })
    } catch (error) {
      this.setData({ evaluationState: 'error' })
      showError(error)
    } finally {
      this.setData({ loadingTargets: false })
    }
  },

  async evaluate(event: WechatMiniprogram.BaseEvent) {
    const revieweeId = String(event.currentTarget.dataset.id)
    const target = this.data.targets.find((item) => item.userId === revieweeId)
    if (!target || target.alreadyEvaluated) return
    const tags = [...new Set(this.data.evaluationTagsText
      .split(/[，,\n]/)
      .map((item) => item.trim())
      .filter(Boolean))]
    const privateNote = this.data.privateNote.trim()
    if (tags.length > 5 || tags.some((item) => Array.from(item).length > 20)) {
      wx.showToast({ title: '最多 5 个标签，每个不超过 20 字', icon: 'none' })
      return
    }
    if (Array.from(privateNote).length > 500) {
      wx.showToast({ title: '私密备注不能超过 500 字', icon: 'none' })
      return
    }
    this.setData({ busyTargetId: revieweeId })
    try {
      await request<EvaluationView>({
        path: `/activities/${this.data.activityId}/evaluations/${revieweeId}`,
        method: 'POST',
        data: { rating: this.data.ratingValue, tags, privateNote: privateNote || null },
      })
      this.setData({
        targets: this.data.targets.map((item) => item.userId === revieweeId ? { ...item, alreadyEvaluated: true } : item),
        evaluationTagsText: '',
        privateNote: '',
      })
      wx.showToast({ title: '评价已提交', icon: 'success' })
      const reputation = await request<ReputationView>({ path: '/me/reputation' })
      this.setData(this.reputationData(reputation))
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ busyTargetId: '' })
    }
  },
})
