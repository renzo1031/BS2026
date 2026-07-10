export const requestStatusOptions = [
  { label: '待受理', value: 'SUBMITTED' },
  { label: '已受理', value: 'ACCEPTED' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已办结', value: 'FINISHED' },
  { label: '已评价', value: 'EVALUATED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' }
]

const requestStatusMap = Object.fromEntries(requestStatusOptions.map(({ label, value }) => [value, label]))

export const itemTypeMap = {
  REPAIR: '宿舍报修',
  CERTIFICATE: '在读证明',
  VENUE: '活动场地'
}

export function statusText(status) {
  return requestStatusMap[status] || status || '-'
}

export function statusType(status) {
  if (['REJECTED', 'CANCELLED'].includes(status)) return 'error'
  if (['FINISHED', 'EVALUATED'].includes(status)) return 'success'
  if (status === 'ACCEPTED') return 'info'
  return 'warning'
}

export function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false
  }).format(date)
}
