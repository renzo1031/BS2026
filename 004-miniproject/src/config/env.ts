export const ENV = {
  apiBaseUrl: 'http://127.0.0.1:8080/api/v1',
  socketUrl: 'ws://127.0.0.1:8080/ws',
  requestTimeoutMs: 15_000,
  socketHeartbeatMs: 25_000,
  socketMaxBackoffMs: 30_000,
} as const

// 正式发布前必须替换为已在微信公众平台登记的 HTTPS/WSS 合法域名。
