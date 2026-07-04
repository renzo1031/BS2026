import axios from 'axios'
import type { ApiResult } from '../types'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 12000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export async function request<T>(config: Parameters<typeof http.request<ApiResult<T>>>[0]): Promise<T> {
  const response = await http.request<ApiResult<T>>(config)
  const result = response.data
  if (result.code !== 200) {
    throw new Error(result.message || '请求失败')
  }
  return result.data
}
