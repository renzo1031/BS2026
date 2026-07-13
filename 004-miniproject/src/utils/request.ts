import { ENV } from '../config/env'
import { session } from './session'
import type { ApiResponse, Tokens } from '../types/api'

// wx.request 不支持 PATCH；需要局部更新的接口由服务端同时提供 PUT 语义。
type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export class ApiError extends Error {
  constructor(public readonly code: string, message: string, public readonly statusCode: number) {
    super(message)
  }
}

let refreshPromise: Promise<void> | null = null

function rawRequest<T>(options: { path: string; method?: HttpMethod; data?: WechatMiniprogram.IAnyObject; authenticated?: boolean }) {
  return new Promise<T>((resolve, reject) => {
    const header: Record<string, string> = { 'content-type': 'application/json' }
    if (options.authenticated !== false && session.accessToken) header.Authorization = `Bearer ${session.accessToken}`
    wx.request<ApiResponse<T>>({
      url: `${ENV.apiBaseUrl}${options.path}`,
      method: options.method || 'GET',
      data: options.data,
      header,
      timeout: ENV.requestTimeoutMs,
      success(response) {
        const body = response.data
        if (response.statusCode >= 200 && response.statusCode < 300 && body?.code === 'OK') {
          resolve(body.data)
          return
        }
        reject(new ApiError(body?.code || 'HTTP_ERROR', body?.message || '请求失败', response.statusCode))
      },
      fail(error) {
        reject(new ApiError('NETWORK_ERROR', error.errMsg || '网络连接失败', 0))
      },
    })
  })
}

async function refresh() {
  if (!session.refreshToken) throw new ApiError('UNAUTHENTICATED', '登录状态已失效', 401)
  const tokens = await rawRequest<Tokens>({
    path: '/auth/refresh',
    method: 'POST',
    authenticated: false,
    data: { refreshToken: session.refreshToken },
  })
  session.save(tokens)
}

export async function request<T>(options: { path: string; method?: HttpMethod; data?: WechatMiniprogram.IAnyObject; retry?: boolean }): Promise<T> {
  try {
    return await rawRequest<T>(options)
  } catch (error) {
    if (!(error instanceof ApiError) || error.statusCode !== 401 || options.retry === false || !session.refreshToken) throw error
    refreshPromise ??= refresh().finally(() => { refreshPromise = null })
    try {
      await refreshPromise
    } catch (refreshError) {
      session.clear()
      throw refreshError
    }
    return rawRequest<T>({ ...options, authenticated: true })
  }
}

function rawUploadImage(filePath: string, businessType: string, campusId?: string) {
  return new Promise<{ id: string; status: string }>((resolve, reject) => {
    wx.uploadFile({
      url: `${ENV.apiBaseUrl}/files`,
      filePath,
      name: 'file',
      formData: { businessType, ...(campusId ? { campusId } : {}) },
      header: { Authorization: `Bearer ${session.accessToken}` },
      timeout: 30_000,
      success(response) {
        try {
          const body = JSON.parse(response.data) as ApiResponse<{ id: string; status: string }>
          if (response.statusCode >= 200 && response.statusCode < 300 && body.code === 'OK') resolve(body.data)
          else reject(new ApiError(body.code || 'UPLOAD_ERROR', body.message || '上传失败', response.statusCode))
        } catch {
          reject(new ApiError('UPLOAD_ERROR', '上传响应格式不正确', response.statusCode))
        }
      },
      fail(error) { reject(new ApiError('NETWORK_ERROR', error.errMsg || '上传失败', 0)) },
    })
  })
}

export async function uploadImage(filePath: string, businessType: string, campusId?: string) {
  try {
    return await rawUploadImage(filePath, businessType, campusId)
  } catch (error) {
    if (!(error instanceof ApiError) || error.statusCode !== 401 || !session.refreshToken) throw error
    refreshPromise ??= refresh().finally(() => { refreshPromise = null })
    try {
      await refreshPromise
    } catch (refreshError) {
      session.clear()
      throw refreshError
    }
    return rawUploadImage(filePath, businessType, campusId)
  }
}

export function showError(error: unknown) {
  wx.showToast({ title: error instanceof Error ? error.message : '操作失败', icon: 'none', duration: 2600 })
}
