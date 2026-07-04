export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface User {
  id: number
  username: string
  realName: string
  phone: string
  studentNo?: string
  email?: string
  status: string
}

export interface Category {
  id: number
  categoryName: string
  status: string
  sortOrder: number
}

export interface Location {
  id: number
  locationName: string
  areaName?: string
  status: string
  sortOrder: number
}

export interface Item {
  id: number
  itemNo: string
  type: 'LOST' | 'FOUND'
  title: string
  categoryId: number
  locationId: number
  eventTime: string
  description: string
  contactName: string
  contactPhone: string
  status: string
  publisherId: number
  reviewerId?: number
  reviewTime?: string
  reviewResult?: string
  reviewReason?: string
  currentClaimantId?: number
  custodianId?: number
  custodyLocation?: string
  lastOperatorId?: number
  lastOperationSummary?: string
  lastOperationTime?: string
  createdAt?: string
  updatedAt?: string
}

export interface AdminItemRow extends Item {
  publisherName?: string
  publisherUsername?: string
  publisherPhone?: string
  claimantName?: string
  claimantUsername?: string
  claimantPhone?: string
  reviewerName?: string
  custodianName?: string
  lastOperatorName?: string
}

export interface Claim {
  id: number
  itemId: number
  applicantId: number
  applicantName: string
  applicantPhone: string
  proofText: string
  status: string
  reviewReason?: string
  createdAt?: string
}

export interface Clue {
  id: number
  itemId: number
  itemNo?: string
  itemTitle?: string
  submitterId: number
  submitterName?: string
  submitterPhone?: string
  clueContent: string
  contactPhone?: string
  status: string
  confirmerName?: string
  confirmReason?: string
  confirmTime?: string
  createdAt?: string
}

export interface Notice {
  id: number
  noticeType: string
  title: string
  content: string
  readStatus?: string
  publishStatus?: string
  publishedAt?: string
  startTime?: string
  endTime?: string
  popupEnabled?: number
}

export interface OperationLog {
  id: number
  operatorId?: number
  operatorName?: string
  operatorRole?: string
  targetType: string
  targetId?: number
  action: string
  beforeStatus?: string
  afterStatus?: string
  result: string
  reason?: string
  requestIp?: string
  requestPath?: string
  createdAt: string
}
