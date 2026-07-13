export type UserRole = 'PLATFORM_ADMIN' | 'CAMPUS_REVIEWER' | 'STUDENT'

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  requestId?: string
  timestamp: string
}

export interface PageResult<T> {
  records: T[]
  total: string
  page: string
  size: string
}

export interface UserView {
  id: string
  campusId?: string
  campusName?: string
  username?: string
  wechatOpenid?: string
  avatarFileId?: string
  bio?: string
  gradeName?: string
  majorName?: string
  interestTagsJson?: string
  nickname: string
  role: UserRole
  status: 'ACTIVE' | 'LIMITED' | 'SUSPENDED' | 'CLOSED'
  verificationStatus: string
  tokenVersion?: number
  createdAt?: string
  updatedAt?: string
}

export interface Tokens {
  accessToken: string
  refreshToken: string
  expiresAt: string
  user: UserView
}

export interface ActivityView {
  id: string
  campusId: string
  creatorId: string
  sceneName: string
  title: string
  description: string
  meetingMode: 'ONLINE' | 'OFFLINE' | 'HYBRID'
  publicLocation?: string
  memberLocationDetail?: string
  joinRequirement?: string
  joinQuestions: string[]
  startAt: string
  endAt: string
  applyDeadline: string
  capacity: number
  acceptedCount: number
  reviewStatus: string
  lifecycleStatus: string
  moderationStatus: string
  reviewerId?: string
  claimExpiresAt?: string
  reviewReason?: string
  completionDeadlineAt?: string
  version: number
  tags: string[]
  mediaIds: string[]
  createdAt: string
  updatedAt?: string
}

export interface PersonSummary {
  id: string
  nickname: string
  gradeName?: string
  majorName?: string
  avatarFileId?: string
}

export interface ActivityApplicationSummary {
  id: string
  applicant: PersonSummary
  answers: string[]
  message?: string
  status: string
  decisionReason?: string
  version: number
  createdAt: string
}

export interface ActivityParticipantSummary {
  user: PersonSummary
  memberRole: string
  status: string
  completionStatus?: string
  joinedAt?: string
  leftAt?: string
}

export interface ActivityMediaSummary {
  id: string
  originalName?: string
  contentType: string
  byteSize: number
  width?: number
  height?: number
  status: string
  sortOrder: number
  createdAt: string
}

export interface ActivityTimelineEntry {
  source: 'STATUS' | 'AUDIT' | 'GOVERNANCE'
  dimension: string
  actionName: string
  fromStatus?: string
  toStatus?: string
  reason?: string
  operatorName?: string
  operatorRole?: string
  createdAt: string
}

export interface ActivityReviewDetail {
  activity: ActivityView
  campusName?: string
  creator: PersonSummary
  applications: ActivityApplicationSummary[]
  participants: ActivityParticipantSummary[]
  media: ActivityMediaSummary[]
  timeline: ActivityTimelineEntry[]
}

export interface IdentityBinding {
  id: string
  userId: string
  campusId: string
  identifierType: string
  identifierMasked: string
  proofFileId?: string
  status: string
  reviewReason?: string
  version: number
}

export interface IdentityReview {
  binding: IdentityBinding
  identifierPlaintext: string
}

export interface ReportView {
  id: string
  campusId: string
  targetType: 'ACTIVITY' | 'USER'
  targetId: string
  reasonCode: string
  description?: string
  status: string
  resolution?: string
  appealReason?: string
  appealResolution?: string
  version: number
  createdAt: string
  updatedAt: string
}

export interface ReportReview {
  report: ReportView
  reporterId: string
  assigneeId?: string
}

export interface FileView {
  id: string
  ownerId: string
  campusId: string
  businessType: string
  businessId?: string
  originalName?: string
  contentType: string
  byteSize: string
  width: number
  height: number
  status: string
  scanResult?: string
  sortOrder: number
}

export interface AuditView {
  id: string
  operatorId?: string
  operatorRole?: string
  campusId?: string
  actionName: string
  targetType: string
  targetId?: string
  beforeState?: string
  afterState?: string
  reason?: string
  requestId?: string
  ipAddress?: string
  createdAt: string
}

export interface CampusView {
  id: string
  name: string
  code: string
  status: 'ACTIVE' | 'INACTIVE'
  identityLabel: string
  createdAt?: string
  updatedAt?: string
}

export interface TagView {
  id: string
  campusId: string
  campusName?: string
  name: string
  normalizedName: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt?: string
}
