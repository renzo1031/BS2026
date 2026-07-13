export function formatDateTime(value?: string) {
  if (!value) return '未提供'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function queryString(values: Record<string, string | number | boolean | undefined>) {
  const parts = Object.entries(values)
    .filter(([, value]) => value !== undefined && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
  return parts.length ? `?${parts.join('&')}` : ''
}

export function statusLabel(status?: string) {
  const labels: Record<string, string> = {
    DRAFT: '草稿', PENDING: '待处理', CLAIMED: '审核中', APPROVED: '已通过', REJECTED: '已驳回',
    RECRUITING: '招募中', IN_PROGRESS: '进行中', COMPLETION_PENDING: '待确认完成',
    COMPLETED: '已完成', CANCELLED: '已取消', EXPIRED: '已过期', ACCEPTED: '已接受', WITHDRAWN: '已撤回',
    NORMAL: '正常', HIDDEN: '已隐藏', LIMITED: '受限', SUSPENDED: '已停用', APPEALED: '申诉中',
  }
  return status ? labels[status] || status : '未知状态'
}
