export type UserRole = 'STUDENT' | 'CAMPUS_REVIEWER' | 'PLATFORM_ADMIN'

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
  campusId: string | null
  nickname: string
  avatarFileId?: string | null
  role: UserRole
  status: 'ACTIVE' | 'LIMITED' | 'SUSPENDED' | 'CLOSED'
  verificationStatus: 'UNVERIFIED' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'REVOKED'
  bio?: string | null
  gradeName?: string | null
  majorName?: string | null
  interestTagsJson?: string | null
}

export interface CampusView {
  id: string
  name: string
  code: string
  identityLabel: string
}

export interface IdentityView {
  id: string
  userId: string
  campusId: string
  identifierType: string
  identifierMasked: string
  proofFileId: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'REVOKED'
  reviewReason: string | null
  version: number
}

export interface Tokens {
  accessToken: string
  refreshToken: string
  expiresAt: string
  user: UserView
}

export interface Activity {
  id: string
  campusId: string
  creatorId: string
  sceneName: string
  title: string
  description: string
  meetingMode: 'ONLINE' | 'OFFLINE' | 'HYBRID'
  publicLocation: string | null
  memberLocationDetail: string | null
  joinRequirement: string | null
  joinQuestions: string[]
  startAt: string
  endAt: string
  applyDeadline: string
  capacity: number
  acceptedCount: number
  reviewStatus: string
  lifecycleStatus: string
  moderationStatus: string
  reviewerId: string | null
  claimExpiresAt: string | null
  reviewReason: string | null
  version: number
  tags: string[]
  mediaIds: string[]
  createdAt: string
}

export interface ApplicationView {
  id: string
  activityId: string
  applicantId: string
  applicantNickname: string
  answers: string[]
  message: string | null
  status: string
  decisionReason: string | null
  version: number
  createdAt: string
}

export interface ConversationView {
  id: string
  activityId: string
  activityTitle: string
  status: string
  lastMessageId: string | null
  updatedAt: string
}

export interface MessageView {
  id: string
  conversationId: string
  senderId: string
  senderNickname: string
  clientMessageId: string
  messageType: 'TEXT' | 'IMAGE' | 'SYSTEM'
  content: string | null
  fileId: string | null
  createdAt: string
  duplicate: boolean
}

export interface Overview {
  user: UserView
  publishedCount: string
  joinedCount: string
  pendingApplicationCount: string
  unreadNotificationCount: string
}

export interface FileView {
  id: string
  businessId?: string | null
  status: string
  businessType: string
  scanResult?: string | null
  createdAt?: string
}

export interface NotificationView {
  id: string
  type: string
  title: string
  content: string
  targetType: string | null
  targetId: string | null
  read: boolean
  createdAt: string
}

export interface FavoriteView {
  activityId: string
  sceneName: string
  title: string
  startAt: string
  lifecycleStatus: string
  moderationStatus: string
  available: boolean
}

export interface EvaluationView {
  id: string
  activityId: string
  rating: number
  tags: string[]
  createdAt: string
}

export interface ReputationView {
  receivedCount: string
  averageRating: number
  distribution: Record<string, string>
  recent: EvaluationView[]
}

export interface EvaluationTarget {
  userId: string
  nickname: string
  alreadyEvaluated: boolean
}

export interface ReportView {
  id: string
  campusId: string
  targetType: 'ACTIVITY' | 'USER'
  targetId: string
  reasonCode: string
  description: string | null
  status: string
  resolution: string | null
  appealReason: string | null
  appealResolution: string | null
  version: number
  createdAt: string
  updatedAt: string
}

export interface BlockedUserView {
  id: string
  nickname: string
}
