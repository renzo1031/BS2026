import { request } from './http'
import type { AdminItemRow, Category, Claim, Clue, Item, Location, Notice, OperationLog, PageResult, User } from '../types'

export const authApi = {
  login: (data: Record<string, unknown>) => request<{ token: string; user: User; roles: string[] }>({ url: '/auth/login', method: 'POST', data }),
  register: (data: Record<string, unknown>) => request<{ token: string; user: User; roles: string[] }>({ url: '/auth/register', method: 'POST', data }),
  me: () => request<{ user: User; roles: string[] }>({ url: '/auth/me', method: 'GET' }),
  updateProfile: (data: Record<string, unknown>) => request<User>({ url: '/auth/me', method: 'PUT', data }),
  changePassword: (data: Record<string, unknown>) => request<void>({ url: '/auth/me/password', method: 'PUT', data })
}

export const catalogApi = {
  categories: () => request<Category[]>({ url: '/catalog/categories', method: 'GET' }),
  locations: () => request<Location[]>({ url: '/catalog/locations', method: 'GET' }),
  notices: () => request<Notice[]>({ url: '/catalog/notices', method: 'GET' })
}

export const itemApi = {
  page: (params: Record<string, unknown>) => request<PageResult<Item>>({ url: '/items', method: 'GET', params }),
  detail: (id: number) => request<Record<string, unknown>>({ url: `/items/${id}`, method: 'GET' }),
  create: (data: Record<string, unknown>) => request<Item>({ url: '/items', method: 'POST', data }),
  update: (id: number, data: Record<string, unknown>) => request<Item>({ url: `/items/${id}`, method: 'PUT', data }),
  submit: (id: number) => request<void>({ url: `/items/${id}/submit`, method: 'POST' }),
  mine: (params: Record<string, unknown>) => request<PageResult<Item>>({ url: '/users/me/items', method: 'GET', params })
}

export const claimApi = {
  create: (itemId: number, data: Record<string, unknown>) => request<Claim>({ url: `/items/${itemId}/claims`, method: 'POST', data }),
  mine: (params: Record<string, unknown>) => request<PageResult<Claim>>({ url: '/users/me/claims', method: 'GET', params }),
  staffPage: (params: Record<string, unknown>) => request<PageResult<Claim>>({ url: '/staff/claims', method: 'GET', params }),
  approve: (id: number, data: Record<string, unknown>) => request<void>({ url: `/staff/claims/${id}/approve`, method: 'POST', data }),
  reject: (id: number, data: Record<string, unknown>) => request<void>({ url: `/staff/claims/${id}/reject`, method: 'POST', data }),
  handover: (id: number, data: Record<string, unknown>) => request<void>({ url: `/staff/items/${id}/handover`, method: 'POST', data }),
  updateCustody: (id: number, data: Record<string, unknown>) => request<void>({ url: `/staff/items/${id}/custody`, method: 'PUT', data })
}

export const clueApi = {
  create: (itemId: number, data: Record<string, unknown>) => request<Clue>({ url: `/items/${itemId}/clues`, method: 'POST', data }),
  mine: (params: Record<string, unknown>) => request<PageResult<Clue>>({ url: '/users/me/clues', method: 'GET', params }),
  confirm: (id: number, data: Record<string, unknown>) => request<void>({ url: `/clues/${id}/confirm`, method: 'POST', data })
}

export const adminApi = {
  statistics: () => request<Record<string, number>>({ url: '/admin/statistics', method: 'GET' }),
  items: (params: Record<string, unknown>) => request<PageResult<AdminItemRow>>({ url: '/admin/items', method: 'GET', params }),
  itemDetail: (id: number) => request<Record<string, unknown>>({ url: `/admin/items/${id}`, method: 'GET' }),
  review: (id: number, data: Record<string, unknown>) => request<void>({ url: `/admin/items/${id}/review`, method: 'POST', data }),
  offline: (id: number, data: Record<string, unknown>) => request<void>({ url: `/admin/items/${id}/offline`, method: 'POST', data }),
  archive: (id: number, data: Record<string, unknown>) => request<void>({ url: `/admin/items/${id}/archive`, method: 'POST', data }),
  claims: (params: Record<string, unknown>) => request<PageResult<Claim>>({ url: '/admin/claims', method: 'GET', params }),
  clues: (params: Record<string, unknown>) => request<PageResult<Clue>>({ url: '/admin/clues', method: 'GET', params }),
  users: (params: Record<string, unknown>) => request<PageResult<User>>({ url: '/admin/users', method: 'GET', params }),
  updateUserStatus: (id: number, data: Record<string, unknown>) => request<void>({ url: `/admin/users/${id}/status`, method: 'PUT', data }),
  resetUserPassword: (id: number, data: Record<string, unknown>) => request<void>({ url: `/admin/users/${id}/password/reset`, method: 'POST', data }),
  categories: (params: Record<string, unknown>) => request<PageResult<Category>>({ url: '/admin/categories', method: 'GET', params }),
  saveCategory: (data: Record<string, unknown>, id?: number) => request<Category>({ url: id ? `/admin/categories/${id}` : '/admin/categories', method: id ? 'PUT' : 'POST', data }),
  locations: (params: Record<string, unknown>) => request<PageResult<Location>>({ url: '/admin/locations', method: 'GET', params }),
  saveLocation: (data: Record<string, unknown>, id?: number) => request<Location>({ url: id ? `/admin/locations/${id}` : '/admin/locations', method: id ? 'PUT' : 'POST', data }),
  notices: (params: Record<string, unknown>) => request<PageResult<Notice>>({ url: '/admin/notices', method: 'GET', params }),
  saveNotice: (data: Record<string, unknown>, id?: number) => request<Notice>({ url: id ? `/admin/notices/${id}` : '/admin/notices', method: id ? 'PUT' : 'POST', data }),
  deleteNotice: (id: number) => request<void>({ url: `/admin/notices/${id}`, method: 'DELETE' }),
  logs: (params: Record<string, unknown>) => request<PageResult<OperationLog>>({ url: '/admin/logs', method: 'GET', params })
}
