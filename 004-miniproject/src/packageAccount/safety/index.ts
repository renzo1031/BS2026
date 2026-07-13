import type { BlockedUserView, PageResult, ReportView } from '../../types/api'
import { formatDateTime } from '../../utils/format'
import { ensureAuthenticated, ensureVerified } from '../../utils/navigation'
import { request, showError } from '../../utils/request'
import { session } from '../../utils/session'

type ReportRow = ReportView & { createdAtText: string; statusText: string; targetText: string; reasonText: string }
type SafetyCard = { label: string; value: string; note: string }

const pageSize = 10
const decimalId = /^[1-9]\d*$/
const statusLabels: Record<string, string> = {
  SUBMITTED: '已提交', REVIEWING: '处理中', ACTIONED: '已处置', DISMISSED: '未采纳',
  APPEALED: '申诉复核中', UPHELD: '维持处置', REVOKED: '已撤销处置',
}
const reasonLabels: Record<string, string> = {
  HARASSMENT: '人身攻击或骚扰', FRAUD: '欺诈或虚假信息', INAPPROPRIATE_CONTENT: '不当内容',
  SAFETY_RISK: '安全风险', OTHER: '其他违规',
}

function reportRow(item: ReportView): ReportRow {
  return {
    ...item,
    createdAtText: formatDateTime(item.createdAt),
    statusText: statusLabels[item.status] || item.status,
    targetText: `${item.targetType === 'ACTIVITY' ? '活动' : '用户'} #${item.targetId}`,
    reasonText: reasonLabels[item.reasonCode] || '其他违规',
  }
}

Page({
  data: {
    tab: 'submit',
    state: 'loading',
    canSubmit: false,
    targetTypeLabels: ['活动', '用户'],
    targetTypeValues: ['ACTIVITY', 'USER'],
    targetTypeIndex: 0,
    targetType: 'ACTIVITY',
    targetId: '',
    reasonLabels: ['人身攻击或骚扰', '欺诈或虚假信息', '不当内容', '安全风险', '其他违规'],
    reasonValues: ['HARASSMENT', 'FRAUD', 'INAPPROPRIATE_CONTENT', 'SAFETY_RISK', 'OTHER'],
    reasonIndex: 0,
    description: '',
    submitting: false,
    caseKind: 'mine',
    mine: [] as ReportRow[],
    affected: [] as ReportRow[],
    minePage: 1,
    affectedPage: 1,
    mineTotal: 0,
    affectedTotal: 0,
    mineHasMore: false,
    affectedHasMore: false,
    loadingCases: false,
    safetyCards: [] as SafetyCard[],
    appealId: '',
    appealReason: '',
    appealing: false,
    blocks: [] as BlockedUserView[],
    blockId: '',
    blockBusy: false,
  },

  onLoad(options: Record<string, string | undefined>) {
    if (!ensureAuthenticated()) return
    const targetType = options.targetType === 'USER' ? 'USER' : 'ACTIVITY'
    this.setData({
      targetType,
      targetTypeIndex: targetType === 'USER' ? 1 : 0,
      targetId: options.targetId || '',
      canSubmit: session.user?.status === 'ACTIVE' && session.user.verificationStatus === 'APPROVED',
    })
    void this.load()
  },

  onPullDownRefresh() {
    void this.load().finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.tab === 'cases') void this.loadMoreCases()
  },

  async load() {
    if (!ensureAuthenticated()) return
    this.setData({ state: 'loading' })
    try {
      const [mine, affected, blocks] = await Promise.all([
        request<PageResult<ReportView>>({ path: `/reports/mine?page=1&size=${pageSize}` }),
        request<PageResult<ReportView>>({ path: `/reports/affected?page=1&size=${pageSize}` }),
        request<BlockedUserView[]>({ path: '/blocks' }),
      ])
      const mineTotal = Number(mine.total)
      const affectedTotal = Number(affected.total)
      this.setData({
        mine: mine.records.map(reportRow),
        affected: affected.records.map(reportRow),
        blocks,
        minePage: 1,
        affectedPage: 1,
        mineTotal,
        affectedTotal,
        mineHasMore: mine.records.length < mineTotal,
        affectedHasMore: affected.records.length < affectedTotal,
        safetyCards: [
          { label: '我的举报', value: String(mineTotal), note: '已提交记录' },
          { label: '影响我的', value: String(affectedTotal), note: '处置与申诉' },
          { label: '黑名单', value: String(blocks.length), note: '已屏蔽用户' },
        ],
        canSubmit: session.user?.status === 'ACTIVE' && session.user.verificationStatus === 'APPROVED',
        state: 'ready',
      })
    } catch (error) {
      this.setData({ state: 'error' })
      showError(error)
    }
  },

  switchTab(event: WechatMiniprogram.BaseEvent) {
    this.setData({ tab: String(event.currentTarget.dataset.tab) })
  },

  switchCaseKind(event: WechatMiniprogram.BaseEvent) {
    this.setData({ caseKind: String(event.currentTarget.dataset.kind), appealId: '', appealReason: '' })
  },

  onInput(event: WechatMiniprogram.Input) {
    this.setData({ [String(event.currentTarget.dataset.field)]: event.detail.value })
  },

  onTargetTypeChange(event: WechatMiniprogram.PickerChange) {
    const targetTypeIndex = Number(event.detail.value)
    this.setData({
      targetTypeIndex,
      targetType: this.data.targetTypeValues[targetTypeIndex] || 'ACTIVITY',
    })
  },

  onReasonChange(event: WechatMiniprogram.PickerChange) {
    this.setData({ reasonIndex: Number(event.detail.value) })
  },

  async submitReport() {
    if (!ensureVerified()) return
    if (!this.data.canSubmit) {
      wx.showToast({ title: '当前账号状态不能提交新举报', icon: 'none' })
      return
    }
    const targetId = this.data.targetId.trim()
    const description = this.data.description.trim()
    if (!decimalId.test(targetId)) {
      wx.showToast({ title: '请输入有效的活动或用户 ID', icon: 'none' })
      return
    }
    if (this.data.targetType === 'USER' && targetId === session.user?.id) {
      wx.showToast({ title: '不能举报自己', icon: 'none' })
      return
    }
    if (Array.from(description).length > 1000) {
      wx.showToast({ title: '举报说明不能超过 1000 字', icon: 'none' })
      return
    }
    const reasonCode = this.data.reasonValues[this.data.reasonIndex]
    if (!reasonCode) return
    this.setData({ submitting: true })
    try {
      const report = await request<ReportView>({
        path: '/reports',
        method: 'POST',
        data: { targetType: this.data.targetType, targetId, reasonCode, description: description || null },
      })
      const mineTotal = this.data.mineTotal + 1
      const mine = [reportRow(report), ...this.data.mine].slice(0, pageSize)
      this.setData({
        mine,
        minePage: 1,
        mineTotal,
        mineHasMore: mine.length < mineTotal,
        tab: 'cases',
        caseKind: 'mine',
        targetId: '',
        description: '',
      })
      wx.showToast({ title: '举报已提交', icon: 'success' })
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ submitting: false })
    }
  },

  async loadMoreCases() {
    if (this.data.loadingCases) return
    const mine = this.data.caseKind === 'mine'
    if (mine ? !this.data.mineHasMore : !this.data.affectedHasMore) return
    const nextPage = (mine ? this.data.minePage : this.data.affectedPage) + 1
    this.setData({ loadingCases: true })
    try {
      const result = await request<PageResult<ReportView>>({
        path: `${mine ? '/reports/mine' : '/reports/affected'}?page=${nextPage}&size=${pageSize}`,
      })
      if (mine) {
        const rows = [...this.data.mine, ...result.records.map(reportRow)]
        const total = Number(result.total)
        this.setData({ mine: rows, minePage: nextPage, mineTotal: total, mineHasMore: rows.length < total })
      } else {
        const rows = [...this.data.affected, ...result.records.map(reportRow)]
        const total = Number(result.total)
        this.setData({ affected: rows, affectedPage: nextPage, affectedTotal: total, affectedHasMore: rows.length < total })
      }
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ loadingCases: false })
    }
  },

  openTarget(event: WechatMiniprogram.BaseEvent) {
    if (event.currentTarget.dataset.type !== 'ACTIVITY') return
    wx.navigateTo({ url: `/pages/activity-detail/index?id=${String(event.currentTarget.dataset.id)}` })
  },

  startAppeal(event: WechatMiniprogram.BaseEvent) {
    const appealId = String(event.currentTarget.dataset.id)
    const report = this.data.affected.find((item) => item.id === appealId)
    if (!report || report.status !== 'ACTIONED') return
    this.setData({ appealId, appealReason: '' })
  },

  cancelAppeal() {
    this.setData({ appealId: '', appealReason: '' })
  },

  async submitAppeal() {
    const report = this.data.affected.find((item) => item.id === this.data.appealId)
    const reason = this.data.appealReason.trim()
    if (!report || report.status !== 'ACTIONED') return
    if (Array.from(reason).length < 2 || Array.from(reason).length > 1000) {
      wx.showToast({ title: '申诉理由须为 2 到 1000 字', icon: 'none' })
      return
    }
    this.setData({ appealing: true })
    try {
      const updated = await request<ReportView>({
        path: `/reports/${report.id}/appeal`,
        method: 'POST',
        data: { version: report.version, reason },
      })
      this.setData({
        affected: this.data.affected.map((item) => item.id === updated.id ? reportRow(updated) : item),
        appealId: '',
        appealReason: '',
      })
      wx.showToast({ title: '申诉已提交', icon: 'success' })
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ appealing: false })
    }
  },

  async addBlock() {
    if (!ensureVerified()) return
    if (!this.data.canSubmit) {
      wx.showToast({ title: '当前账号状态不能新增黑名单', icon: 'none' })
      return
    }
    const blockedId = this.data.blockId.trim()
    if (!decimalId.test(blockedId) || blockedId === session.user?.id) {
      wx.showToast({ title: '请输入有效且非本人的用户 ID', icon: 'none' })
      return
    }
    this.setData({ blockBusy: true })
    try {
      await request<void>({ path: `/blocks/${blockedId}`, method: 'POST', data: {} })
      const blocks = await request<BlockedUserView[]>({ path: '/blocks' })
      this.setData({ blocks, blockId: '' })
      wx.showToast({ title: '已加入黑名单', icon: 'success' })
    } catch (error) {
      showError(error)
    } finally {
      this.setData({ blockBusy: false })
    }
  },

  async removeBlock(event: WechatMiniprogram.BaseEvent) {
    const blockedId = String(event.currentTarget.dataset.id)
    const modal = await wx.showModal({ title: '移出黑名单', content: '确定恢复与该用户的互动权限吗？' })
    if (!modal.confirm) return
    try {
      await request<void>({ path: `/blocks/${blockedId}`, method: 'DELETE' })
      this.setData({ blocks: this.data.blocks.filter((item) => item.id !== blockedId) })
      wx.showToast({ title: '已移出黑名单', icon: 'success' })
    } catch (error) {
      showError(error)
    }
  },
})
