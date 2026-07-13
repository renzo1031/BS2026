import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import type { ApiResponse, Tokens, UserView } from '@/types/api'

const ACCESS_KEY = 'campus-buddy-admin-access'
const REFRESH_KEY = 'campus-buddy-admin-refresh'

export const AUTH_REFRESHED_EVENT = 'auth-refreshed'
export const AUTH_EXPIRED_EVENT = 'auth-expired'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15_000,
})

export const tokenStorage = {
  access: () => sessionStorage.getItem(ACCESS_KEY),
  refresh: () => sessionStorage.getItem(REFRESH_KEY),
  save(tokens: Pick<Tokens, 'accessToken' | 'refreshToken'>) {
    sessionStorage.setItem(ACCESS_KEY, tokens.accessToken)
    sessionStorage.setItem(REFRESH_KEY, tokens.refreshToken)
  },
  clear() {
    sessionStorage.removeItem(ACCESS_KEY)
    sessionStorage.removeItem(REFRESH_KEY)
  },
}

http.interceptors.request.use((config) => {
  const token = tokenStorage.access()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let refreshPromise: Promise<string> | null = null

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config as (AxiosRequestConfig & { retried?: boolean }) | undefined
    const refreshToken = tokenStorage.refresh()
    if (error.response?.status !== 401 || !original || original.retried || !refreshToken) {
      return Promise.reject(error)
    }
    original.retried = true
    refreshPromise ??= axios
      .post<ApiResponse<Tokens>>(`${http.defaults.baseURL}/auth/refresh`, { refreshToken })
      .then(({ data }) => {
        tokenStorage.save(data.data)
        window.dispatchEvent(new CustomEvent<UserView>(AUTH_REFRESHED_EVENT, { detail: data.data.user }))
        return data.data.accessToken
      })
      .catch((refreshError: unknown) => {
        tokenStorage.clear()
        window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT))
        throw refreshError
      })
      .finally(() => {
        refreshPromise = null
      })
    const accessToken = await refreshPromise
    original.headers = { ...original.headers, Authorization: `Bearer ${accessToken}` }
    return http.request(original)
  },
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResponse<T>>(config)
  return response.data.data
}

export function errorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    return error.response?.data?.message || (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络请求失败')
  }
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}
