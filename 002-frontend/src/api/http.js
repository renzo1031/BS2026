import axios from 'axios'

let unauthorizedHandler = null

export class ApiError extends Error {
  constructor(message, { status = null, code = null, data = null } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.data = data
  }
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

function handleUnauthorized(error) {
  if (error.status === 401) unauthorizedHandler?.(error)
  return Promise.reject(error)
}

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') return response
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body && body.code !== 0) {
      const code = Number(body.code)
      const status = response.status >= 400 ? response.status : (code >= 400 && code < 600 ? code : response.status)
      return handleUnauthorized(new ApiError(body.message || '请求失败', { status, code: body.code, data: body.data }))
    }
    return body?.data
  },
  (error) => {
    const response = error.response
    const body = response?.data
    const message = body && !(body instanceof Blob) ? body.message : null
    return handleUnauthorized(new ApiError(message || error.message || '网络异常', {
      status: response?.status ?? null,
      code: body?.code ?? null,
      data: body?.data ?? null
    }))
  }
)

export default http
