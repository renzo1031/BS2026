import { ENV } from '../config/env'
import { request } from './request'
import { session } from './session'

export interface SocketEvent {
  type: string
  code?: string
  message?: string
  data?: unknown
  clientMessageId?: string
  messageId?: string
  duplicate?: boolean
}

class RealtimeSocket {
  private task: WechatMiniprogram.SocketTask | null = null
  private listeners = new Set<(event: SocketEvent) => void>()
  private heartbeat: number | null = null
  private reconnectTimer: number | null = null
  private attempts = 0
  private manualClose = false
  private connecting: Promise<void> | null = null
  private connectionGeneration = 0

  connect() {
    if (this.connecting) return this.connecting
    if (this.task) return Promise.resolve()
    const generation = ++this.connectionGeneration
    const attempt = this.open(generation)
    let tracked: Promise<void>
    tracked = attempt.finally(() => {
      if (this.connecting === tracked) this.connecting = null
    })
    this.connecting = tracked
    return tracked
  }

  subscribe(listener: (event: SocketEvent) => void) {
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  send(payload: Record<string, unknown>) {
    return new Promise<void>((resolve, reject) => {
      if (!this.task) { reject(new Error('实时连接尚未建立')); return }
      this.task.send({ data: JSON.stringify(payload), success: () => resolve(), fail: reject })
    })
  }

  close() {
    this.manualClose = true
    this.connectionGeneration += 1
    this.connecting = null
    if (this.reconnectTimer !== null) clearTimeout(this.reconnectTimer)
    if (this.heartbeat !== null) clearInterval(this.heartbeat)
    this.reconnectTimer = null
    this.heartbeat = null
    this.attempts = 0
    const task = this.task
    this.task = null
    task?.close({ code: 1000, reason: 'page closed' })
  }

  private async open(generation: number) {
    if (!session.authenticated || generation !== this.connectionGeneration) return
    this.manualClose = false
    let result: { ticket: string }
    try {
      result = await request<{ ticket: string }>({ path: '/ws-ticket', method: 'POST', data: {} })
    } catch (error) {
      if (generation === this.connectionGeneration && !this.manualClose) this.scheduleReconnect()
      throw error
    }
    if (generation !== this.connectionGeneration || this.manualClose) return
    await new Promise<void>((resolve, reject) => {
      const task = wx.connectSocket({ url: `${ENV.socketUrl}?ticket=${encodeURIComponent(result.ticket)}` })
      this.task = task
      let disconnected = false
      const isCurrent = () => generation === this.connectionGeneration && !disconnected && this.task === task
      const disconnect = () => {
        if (!isCurrent()) return
        disconnected = true
        this.task = null
        if (this.heartbeat !== null) clearInterval(this.heartbeat)
        this.heartbeat = null
        this.listeners.forEach((listener) => listener({ type: 'DISCONNECTED' }))
        if (!this.manualClose) this.scheduleReconnect()
      }
      task.onOpen(() => {
        if (!isCurrent()) {
          task.close({ code: 1000, reason: 'stale connection' })
          reject(new Error('实时连接已关闭'))
          return
        }
        this.attempts = 0
        this.startHeartbeat()
        resolve()
      })
      task.onMessage((message) => {
        if (!isCurrent()) return
        try {
          const event = JSON.parse(String(message.data)) as SocketEvent
          this.listeners.forEach((listener) => listener(event))
        } catch { /* Ignore malformed server frames. */ }
      })
      task.onError((error) => {
        disconnect()
        reject(new Error(error.errMsg || '实时连接失败'))
      })
      task.onClose(disconnect)
    })
  }

  private startHeartbeat() {
    if (this.heartbeat !== null) clearInterval(this.heartbeat)
    this.heartbeat = setInterval(() => { void this.send({ type: 'PING' }).catch(() => undefined) }, ENV.socketHeartbeatMs) as unknown as number
  }

  private scheduleReconnect() {
    if (this.reconnectTimer !== null || !session.authenticated) return
    const delay = Math.min(1_000 * 2 ** this.attempts, ENV.socketMaxBackoffMs)
    this.attempts += 1
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      void this.connect().catch(() => this.scheduleReconnect())
    }, delay) as unknown as number
  }
}

export const realtimeSocket = new RealtimeSocket()
