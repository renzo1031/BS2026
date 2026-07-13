import type { Activity } from '../../types/api'
import { ensureVerified } from '../../utils/navigation'
import { request, showError, uploadImage } from '../../utils/request'

const pad = (value: number) => String(value).padStart(2, '0')
const parts = (date: Date) => ({ date: `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`, time: `${pad(date.getHours())}:${pad(date.getMinutes())}` })
const initialStart = new Date(Date.now() + 24 * 60 * 60 * 1000)
const initialEnd = new Date(initialStart.getTime() + 2 * 60 * 60 * 1000)
const initialDeadline = new Date(initialStart.getTime() - 2 * 60 * 60 * 1000)
const templates = [
  { sceneName: '晨跑搭子', title: '找同学一起晨跑打卡', tagsText: '运动，新手友好，早起', joinRequirement: '按时到场，量力而行。', questionsText: '你平时跑步配速或距离大概是多少？' },
  { sceneName: '自习搭子', title: '期末周图书馆自习搭子', tagsText: '学习，安静，自律', joinRequirement: '尽量保持安静，临时有事提前说。', questionsText: '你计划复习哪门课？' },
  { sceneName: '摄影搭子', title: '周末校园纪实片拍摄搭子', tagsText: '摄影，周末，校园', joinRequirement: '欢迎摄影或采访感兴趣的同学。', questionsText: '你更想负责拍摄、出镜还是采访？' },
]

Page({
  data: {
    id: '',
    version: 0,
    state: 'ready',
    saving: false,
    sceneName: '',
    title: '',
    description: '',
    modeIndex: 0,
    modeLabels: ['线下', '线上', '线上 + 线下'],
    modeValues: ['OFFLINE', 'ONLINE', 'HYBRID'],
    publicLocation: '',
    memberLocationDetail: '',
    joinRequirement: '',
    questionsText: '',
    startDate: parts(initialStart).date,
    startTime: parts(initialStart).time,
    endDate: parts(initialEnd).date,
    endTime: parts(initialEnd).time,
    deadlineDate: parts(initialDeadline).date,
    deadlineTime: parts(initialDeadline).time,
    capacity: '4',
    tagsText: '',
    templates,
    selectedPaths: [] as string[],
    mediaIds: [] as string[],
    pendingMediaIds: [] as string[],
  },

  onLoad(options: Record<string, string | undefined>) {
    if (!ensureVerified()) return
    if (options.id) {
      this.setData({ id: options.id, state: 'loading' })
      void this.load()
    }
  },

  async load() {
    try {
      const item = await request<Activity>({ path: `/activities/${this.data.id}` })
      const start = parts(new Date(item.startAt))
      const end = parts(new Date(item.endAt))
      const deadline = parts(new Date(item.applyDeadline))
      const pendingMediaIds = wx.getStorageSync<string[]>(`activityMedia.pending.${item.id}`) || []
      this.setData({
        version: item.version,
        sceneName: item.sceneName,
        title: item.title,
        description: item.description,
        modeIndex: Math.max(0, this.data.modeValues.indexOf(item.meetingMode)),
        publicLocation: item.publicLocation || '',
        memberLocationDetail: item.memberLocationDetail || '',
        joinRequirement: item.joinRequirement || '',
        questionsText: item.joinQuestions.join('\n'),
        startDate: start.date, startTime: start.time,
        endDate: end.date, endTime: end.time,
        deadlineDate: deadline.date, deadlineTime: deadline.time,
        capacity: String(item.capacity),
        tagsText: item.tags.join('，'),
        mediaIds: item.mediaIds,
        pendingMediaIds,
        state: 'ready',
      })
    } catch (error) {
      this.setData({ state: 'error' })
      showError(error)
    }
  },

  onInput(event: WechatMiniprogram.Input) {
    const field = String(event.currentTarget.dataset.field)
    this.setData({ [field]: event.detail.value })
  },
  applyTemplate(event: WechatMiniprogram.BaseEvent) {
    const template = templates[Number(event.currentTarget.dataset.index)]
    if (!template) return
    this.setData({
      ...template,
      description: this.data.description || `我想发起一个「${template.sceneName}」活动，欢迎同校同学一起参与。请在报名时简单说明你的时间安排和参与想法，审核通过后我们再沟通细节。`,
    })
  },
  onModeChange(event: WechatMiniprogram.PickerChange) { this.setData({ modeIndex: Number(event.detail.value) }) },
  onDateChange(event: WechatMiniprogram.PickerChange) { this.setData({ [String(event.currentTarget.dataset.field)]: event.detail.value }) },

  async chooseImages() {
    try {
      const remain = 6 - this.data.mediaIds.length - this.data.pendingMediaIds.length
      if (remain <= 0) { wx.showToast({ title: '最多上传 6 张图片', icon: 'none' }); return }
      const result = await wx.chooseImage({ count: remain, sourceType: ['album', 'camera'] })
      this.setData({ selectedPaths: result.tempFilePaths })
    } catch (error) {
      if (!(error as { errMsg?: string }).errMsg?.includes('cancel')) showError(error)
    }
  },

  payload() {
    const split = (value: string, max: number) => value.split(/[，,\n]/).map((item) => item.trim()).filter(Boolean).slice(0, max)
    return {
      sceneName: this.data.sceneName.trim(),
      title: this.data.title.trim(),
      description: this.data.description.trim(),
      meetingMode: this.data.modeValues[this.data.modeIndex],
      publicLocation: this.data.publicLocation.trim() || null,
      memberLocationDetail: this.data.memberLocationDetail.trim() || null,
      joinRequirement: this.data.joinRequirement.trim() || null,
      joinQuestions: split(this.data.questionsText, 3),
      startAt: new Date(`${this.data.startDate}T${this.data.startTime}:00`).toISOString(),
      endAt: new Date(`${this.data.endDate}T${this.data.endTime}:00`).toISOString(),
      applyDeadline: new Date(`${this.data.deadlineDate}T${this.data.deadlineTime}:00`).toISOString(),
      capacity: Number(this.data.capacity),
      tags: split(this.data.tagsText, 5),
    }
  },

  validate() {
    const payload = this.payload()
    if (payload.sceneName.length < 2 || payload.title.length < 5 || payload.description.length < 20) {
      wx.showToast({ title: '场景、标题或介绍内容过短', icon: 'none' }); return false
    }
    if (!Number.isInteger(payload.capacity) || payload.capacity < 2 || payload.capacity > 50) {
      wx.showToast({ title: '人数须为 2 到 50', icon: 'none' }); return false
    }
    if (payload.meetingMode !== 'ONLINE' && !payload.publicLocation) {
      wx.showToast({ title: '线下活动需填写公开地点', icon: 'none' }); return false
    }
    if (!(new Date(payload.endAt) > new Date(payload.startAt)) || !(new Date(payload.startAt) >= new Date(payload.applyDeadline)) || !(new Date(payload.applyDeadline) > new Date())) {
      wx.showToast({ title: '请检查报名、开始与结束时间', icon: 'none' }); return false
    }
    return true
  },

  async persist() {
    const payload = this.payload()
    const item = this.data.id
      ? await request<Activity>({ path: `/activities/${this.data.id}`, method: 'PUT', data: { version: this.data.version, activity: payload } })
      : await request<Activity>({ path: '/activities', method: 'POST', data: payload })
    this.setData({ id: item.id, version: item.version, mediaIds: item.mediaIds })
    if (this.data.selectedPaths.length) {
      const uploaded: string[] = []
      for (const path of this.data.selectedPaths) uploaded.push((await uploadImage(path, 'ACTIVITY_IMAGE')).id)
      const pendingMediaIds = [...this.data.pendingMediaIds, ...uploaded]
      wx.setStorageSync(`activityMedia.pending.${item.id}`, pendingMediaIds)
      this.setData({ pendingMediaIds, selectedPaths: [] })
    }
    return item
  },

  async attachPending(showFailure: boolean) {
    if (!this.data.pendingMediaIds.length) return true
    try {
      await request<void>({
        path: `/activities/${this.data.id}/media`,
        method: 'PUT',
        data: { fileIds: [...this.data.mediaIds, ...this.data.pendingMediaIds] },
      })
      const mediaIds = [...this.data.mediaIds, ...this.data.pendingMediaIds]
      wx.removeStorageSync(`activityMedia.pending.${this.data.id}`)
      this.setData({ mediaIds, pendingMediaIds: [] })
      return true
    } catch (error) {
      if (showFailure) showError(error)
      return false
    }
  },

  async saveDraft() {
    if (!this.validate()) return
    this.setData({ saving: true })
    try {
      await this.persist()
      const attached = await this.attachPending(false)
      wx.showToast({ title: attached ? '草稿已保存' : '草稿已保存，图片待审核', icon: 'none', duration: 2600 })
    } catch (error) { showError(error) }
    finally { this.setData({ saving: false }) }
  },

  async submitReview() {
    if (!this.validate()) return
    this.setData({ saving: true })
    try {
      const item = await this.persist()
      if (!(await this.attachPending(true))) {
        wx.showToast({ title: '图片审核通过并绑定后才能一起提交', icon: 'none', duration: 3000 })
        return
      }
      const submitted = await request<Activity>({ path: `/activities/${item.id}/submit`, method: 'POST', data: { version: this.data.version } })
      this.setData({ version: submitted.version })
      wx.showToast({ title: '已提交校园审核', icon: 'success' })
      setTimeout(() => wx.redirectTo({ url: '/packageBusiness/my-activities/index' }), 700)
    } catch (error) { showError(error) }
    finally { this.setData({ saving: false }) }
  },
})
