import type { Activity, ConversationView, MessageView } from '../../types/api'
import { formatDateTime, statusLabel } from '../../utils/format'
import { ensureVerified } from '../../utils/navigation'
import { request, showError } from '../../utils/request'
import { session } from '../../utils/session'
import { realtimeSocket, type SocketEvent } from '../../utils/socket'
import { mergeMessagesById } from '../../utils/messages'

type MessageRow = MessageView & { createdAtText: string; mine: boolean }
type WorkspaceActivity = Activity & { lifecycleText: string; modeText: string; timeText: string; locationText: string }
let unsubscribe: (() => void) | null = null
let pendingText = ''
let ackTimer: number | null = null
let activePage: unknown = null
let catchupOwner: unknown = null
let catchupTask: Promise<void> | null = null

Page({
  data: {
    conversationId: '', state: 'loading', conversation: null as ConversationView | null,
    activity: null as WorkspaceActivity | null, messages: [] as MessageRow[], scrollIntoView: '',
    content: '', sending: false, socketReady: false, truncated: false,
  },
  onLoad(options: Record<string, string | undefined>) {
    activePage = this
    if (!ensureVerified()) return
    if (!options.conversationId) { this.setData({ state: 'not-found' }); return }
    this.setData({ conversationId: options.conversationId }); void this.load()
  },
  onUnload() {
    if (activePage === this) activePage = null
    unsubscribe?.(); unsubscribe = null
    realtimeSocket.close()
    if (ackTimer !== null) clearTimeout(ackTimer)
  },
  async load() {
    this.setData({ state: 'loading' })
    try {
      const conversations = await request<ConversationView[]>({ path: '/conversations' })
      const conversation = conversations.find((item) => item.id === this.data.conversationId)
      if (!conversation) { this.setData({ state: 'forbidden' }); return }
      const [activity, messagesResult] = await Promise.all([
        request<Activity>({ path: `/activities/${conversation.activityId}` }),
        this.loadAllMessages(conversation.id),
      ])
      if (activePage !== this) return
      const messages = messagesResult.records.map((item) => this.row(item))
      this.setData({
        conversation,
        activity: {
          ...activity,
          lifecycleText: statusLabel(activity.lifecycleStatus),
          modeText: activity.meetingMode === 'ONLINE' ? '线上' : activity.meetingMode === 'OFFLINE' ? '线下' : '线上 + 线下',
          timeText: `${formatDateTime(activity.startAt)} - ${formatDateTime(activity.endAt)}`,
          locationText: activity.memberLocationDetail || activity.publicLocation || '活动成员内沟通',
        },
        messages,
        truncated: messagesResult.truncated,
        state: 'ready',
        scrollIntoView: messages.length ? `message-${messages[messages.length - 1]!.id}` : '',
      })
      unsubscribe?.()
      unsubscribe = realtimeSocket.subscribe((event) => this.onSocketEvent(event))
      try {
        await realtimeSocket.connect()
        if (activePage === this) this.setData({ socketReady: true })
        else realtimeSocket.close()
      } catch {
        if (activePage === this) this.setData({ socketReady: false })
      }
    } catch (error) {
      if (activePage !== this) return
      this.setData({ state: 'error' }); showError(error)
    }
  },
  async loadAllMessages(conversationId: string, initialAfterId = '0') {
    const records: MessageView[] = []
    let afterId = initialAfterId
    for (let page = 0; page < 10; page += 1) {
      const batch = await request<MessageView[]>({ path: `/conversations/${conversationId}/messages?afterId=${afterId}&limit=100` })
      records.push(...batch)
      if (batch.length < 100) return { records, truncated: false }
      afterId = batch[batch.length - 1]!.id
    }
    return { records, truncated: true }
  },
  catchUpMessages() {
    if (activePage !== this) return Promise.resolve()
    if (catchupTask && catchupOwner === this) return catchupTask
    const conversationId = this.data.conversationId
    const afterId = this.data.messages[this.data.messages.length - 1]?.id || '0'
    const task = (async () => {
      try {
        const result = await this.loadAllMessages(conversationId, afterId)
        if (activePage !== this || this.data.conversationId !== conversationId) return
        const messages = mergeMessagesById(this.data.messages, result.records.map((item) => this.row(item)))
        this.setData({
          messages,
          truncated: this.data.truncated || result.truncated,
          scrollIntoView: messages.length ? `message-${messages[messages.length - 1]!.id}` : '',
        })
      } catch (error) {
        if (activePage === this) showError(error)
      }
    })()
    catchupOwner = this
    catchupTask = task
    void task.finally(() => {
      if (catchupTask === task) {
        catchupTask = null
        catchupOwner = null
      }
    })
    return task
  },
  row(item: MessageView): MessageRow { return { ...item, createdAtText: formatDateTime(item.createdAt), mine: item.senderId === session.user?.id } },
  onInput(event: WechatMiniprogram.Input) { this.setData({ content: event.detail.value }) },
  onSocketEvent(event: SocketEvent) {
    if (event.type === 'DISCONNECTED') {
      this.setData({ socketReady: false })
      return
    }
    if (event.type === 'CONNECTED') {
      this.setData({ socketReady: true })
      void this.catchUpMessages()
      return
    }
    if (event.type === 'MESSAGE') {
      const message = event.data as MessageView
      if (message?.conversationId !== this.data.conversationId || this.data.messages.some((item) => item.id === message.id)) return
      const messages = mergeMessagesById(this.data.messages, [this.row(message)])
      this.setData({ messages, scrollIntoView: `message-${message.id}` })
      return
    }
    if (event.type === 'ACK') {
      pendingText = ''
      if (ackTimer !== null) clearTimeout(ackTimer)
      ackTimer = null
      this.setData({ sending: false })
      return
    }
    if (event.type === 'ERROR') {
      if (ackTimer !== null) clearTimeout(ackTimer)
      ackTimer = null
      this.setData({ sending: false, content: this.data.content || pendingText })
      pendingText = ''
      wx.showToast({ title: event.message || '消息发送失败', icon: 'none' })
    }
  },
  async send() {
    const content = this.data.content.trim()
    if (!content || this.data.sending || this.data.conversation?.status !== 'OPEN') return
    const clientMessageId = `m_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`
    pendingText = content
    this.setData({ content: '', sending: true })
    const payload = { type: 'SEND_MESSAGE', conversationId: this.data.conversationId, clientMessageId, messageType: 'TEXT', content }
    try {
      if (this.data.socketReady) {
        await realtimeSocket.send(payload)
        ackTimer = setTimeout(() => { this.setData({ sending: false, content: pendingText }); pendingText = ''; ackTimer = null }, 8_000) as unknown as number
      } else {
        const message = await request<MessageView>({ path: `/conversations/${this.data.conversationId}/messages`, method: 'POST', data: { clientMessageId, messageType: 'TEXT', content } })
        const messages = [...this.data.messages, this.row(message)]
        pendingText = ''
        this.setData({ messages, sending: false, scrollIntoView: `message-${message.id}` })
      }
    } catch (error) {
      this.setData({ content, sending: false }); pendingText = ''; showError(error)
    }
  },
  async confirmCompletion(event: WechatMiniprogram.BaseEvent) {
    const activity = this.data.activity
    if (!activity) return
    const disputed = event.currentTarget.dataset.disputed === true || event.currentTarget.dataset.disputed === 'true'
    const modal = await wx.showModal({ title: disputed ? '提出异议' : '确认完成', content: disputed ? '当前版本会记录异议，但治理工单能力仍在完善。是否继续？' : '确认你已完成本次活动？' })
    if (!modal.confirm) return
    try {
      await request<Activity>({ path: `/activities/${activity.id}/completion-confirmation`, method: 'POST', data: { disputed } })
      wx.showToast({ title: disputed ? '已记录异议' : '已确认完成', icon: 'success' }); await this.load()
    } catch (error) { showError(error) }
  },
})
