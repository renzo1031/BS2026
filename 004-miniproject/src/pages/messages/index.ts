import type { ConversationView, NotificationView, PageResult } from '../../types/api'
import { formatDateTime } from '../../utils/format'
import { ensureVerified } from '../../utils/navigation'
import { request, showError } from '../../utils/request'

type ConversationRow = ConversationView & { updatedAtText: string }
type NotificationRow = NotificationView & { createdAtText: string; tagText: string }

function notificationTag(item: NotificationView) {
  if (item.type?.includes('REVIEW')) return '审核'
  if (item.type?.includes('APPLICATION')) return '报名'
  if (item.targetType === 'ACTIVITY') return '活动'
  return '系统'
}

Page({
  data: {
    tab: 'conversations',
    state: 'loading',
    conversations: [] as ConversationRow[],
    notifications: [] as NotificationRow[],
    unreadNotifications: [] as NotificationRow[],
    readNotifications: [] as NotificationRow[],
    conversationCount: 0,
    activeConversationCount: 0,
    unreadCount: 0,
  },

  onShow() {
    if (ensureVerified()) void this.load()
  },

  onPullDownRefresh() { void this.load().finally(() => wx.stopPullDownRefresh()) },

  switchTab(event: WechatMiniprogram.BaseEvent) {
    this.setData({ tab: event.currentTarget.dataset.tab })
  },

  async load() {
    this.setData({ state: 'loading' })
    try {
      const [conversations, notifications] = await Promise.all([
        request<ConversationView[]>({ path: '/conversations' }),
        request<PageResult<NotificationView>>({ path: '/notifications?page=1&size=30' }),
      ])
      const conversationRows = conversations.map((item) => ({ ...item, updatedAtText: formatDateTime(item.updatedAt) }))
      const notificationRows = notifications.records.map((item) => ({ ...item, createdAtText: formatDateTime(item.createdAt), tagText: notificationTag(item) }))
      const unreadNotifications = notificationRows.filter((item) => !item.read)
      const readNotifications = notificationRows.filter((item) => item.read)
      this.setData({
        conversations: conversationRows,
        notifications: notificationRows,
        unreadNotifications,
        readNotifications,
        conversationCount: conversationRows.length,
        activeConversationCount: conversationRows.filter((item) => item.status !== 'READ_ONLY').length,
        unreadCount: unreadNotifications.length,
        state: 'ready',
      })
    } catch (error) {
      this.setData({ state: 'error' })
      showError(error)
    }
  },

  openConversation(event: WechatMiniprogram.BaseEvent) {
    wx.navigateTo({ url: `/packageBusiness/workspace/index?conversationId=${event.currentTarget.dataset.id}` })
  },

  async openNotification(event: WechatMiniprogram.BaseEvent) {
    const id = String(event.currentTarget.dataset.id)
    const item = this.data.notifications.find((candidate) => candidate.id === id)
    if (!item) return
    try {
      if (!item.read) await request<void>({ path: `/notifications/${id}/read`, method: 'POST', data: {} })
      if (item.targetType === 'ACTIVITY' && item.targetId) {
        wx.navigateTo({ url: `/pages/activity-detail/index?id=${item.targetId}` })
      } else {
        const notifications = this.data.notifications.map((candidate) => candidate.id === id ? { ...candidate, read: true } : candidate)
        this.updateNotificationGroups(notifications)
      }
    } catch (error) { showError(error) }
  },

  async readAll() {
    try {
      await request<void>({ path: '/notifications/read-all', method: 'POST', data: {} })
      this.updateNotificationGroups(this.data.notifications.map((item) => ({ ...item, read: true })))
    } catch (error) { showError(error) }
  },

  updateNotificationGroups(notifications: NotificationRow[]) {
    const unreadNotifications = notifications.filter((item) => !item.read)
    this.setData({
      notifications,
      unreadNotifications,
      readNotifications: notifications.filter((item) => item.read),
      unreadCount: unreadNotifications.length,
    })
  },
})
