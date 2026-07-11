import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import type { ApiEnvelope } from '@/types/models'

const TOKEN_KEY = 'aid_platform_token'
const USER_KEY = 'aid_platform_user'

const client = axios.create({
  baseURL: '/api',
  timeout: 12000,
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    const status = error.response?.status
    const errorMessage = error.response?.data?.message || (status ? `请求失败（${status}）` : '网络连接失败')
    const isLoginRequest = error.config?.url?.includes('/auth/login') ?? false
    if (status === 401 && !isLoginRequest) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      if (!location.pathname.startsWith('/login')) {
        location.assign(`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`)
      }
    }
    if (status !== 401 || isLoginRequest) {
      message.error(errorMessage)
    }
    return Promise.reject(new Error(errorMessage))
  },
)

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await client.request<ApiEnvelope<T>>(config)
  return response.data.data
}

export const api = {
  get: <T>(url: string, params?: Record<string, unknown>) => request<T>({ method: 'GET', url, params }),
  post: <T>(url: string, data?: unknown) => request<T>({ method: 'POST', url, data }),
  put: <T>(url: string, data?: unknown) => request<T>({ method: 'PUT', url, data }),
  patch: <T>(url: string, data?: unknown) => request<T>({ method: 'PATCH', url, data }),
}

export { TOKEN_KEY, USER_KEY }
