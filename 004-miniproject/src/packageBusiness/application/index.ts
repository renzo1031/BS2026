import type { Activity, ApplicationView } from '../../types/api'
import { formatDateTime } from '../../utils/format'
import { ensureVerified } from '../../utils/navigation'
import { request, showError } from '../../utils/request'

type ActivitySummary = Activity & {
  modeText: string
  remainingSeats: number
}

Page({
  data: {
    activityId: '', state: 'loading', activity: null as ActivitySummary | null,
    startAtText: '', deadlineText: '', answers: [] as string[], message: '', submitting: false,
  },
  onLoad(options: Record<string, string | undefined>) {
    if (!ensureVerified()) return
    if (!options.activityId) { this.setData({ state: 'not-found' }); return }
    this.setData({ activityId: options.activityId })
    void this.load()
  },
  async load() {
    this.setData({ state: 'loading' })
    try {
      const activity = await request<Activity>({ path: `/activities/${this.data.activityId}` })
      this.setData({
        activity: {
          ...activity,
          modeText: activity.meetingMode === 'ONLINE' ? '线上' : activity.meetingMode === 'OFFLINE' ? '线下' : '线上 + 线下',
          remainingSeats: Math.max(0, activity.capacity - activity.acceptedCount),
        },
        answers: activity.joinQuestions.map(() => ''),
        startAtText: formatDateTime(activity.startAt),
        deadlineText: formatDateTime(activity.applyDeadline),
        state: 'ready',
      })
    } catch (error) { this.setData({ state: 'error' }); showError(error) }
  },
  onAnswerInput(event: WechatMiniprogram.Input) {
    const index = Number(event.currentTarget.dataset.index)
    const answers = [...this.data.answers]
    answers[index] = event.detail.value
    this.setData({ answers })
  },
  onMessageInput(event: WechatMiniprogram.Input) { this.setData({ message: event.detail.value }) },
  async submit() {
    if (!this.data.activity || this.data.activity.lifecycleStatus !== 'RECRUITING') { wx.showToast({ title: '活动当前不可申请', icon: 'none' }); return }
    if (this.data.answers.some((answer) => !answer.trim())) { wx.showToast({ title: '请回答全部报名问题', icon: 'none' }); return }
    this.setData({ submitting: true })
    try {
      await request<ApplicationView>({
        path: `/activities/${this.data.activityId}/applications`, method: 'POST',
        data: { answers: this.data.answers.map((item) => item.trim()), message: this.data.message.trim() || null },
      })
      wx.showToast({ title: '申请已提交', icon: 'success' })
      setTimeout(() => wx.redirectTo({ url: '/packageBusiness/my-activities/index' }), 700)
    } catch (error) { showError(error) }
    finally { this.setData({ submitting: false }) }
  },
})
