export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
  traceId: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export type RoleCode = 'SYS_ADMIN' | 'SUPERVISOR' | 'CASE_WORKER' | 'VOLUNTEER'

export interface MeView {
  id: number
  departmentId: number | null
  username: string
  displayName: string
  roleCode: RoleCode
  dataScope: 'GLOBAL' | 'DEPARTMENT' | 'SELF'
  permissions: string[]
}

export interface LoginResponse {
  token: string
  expiresAt: string
  user: MeView
}

export interface PublicOverview {
  completedServices: number
  approvedVolunteers: number
  activeRequests: number
  serviceDepartments: number
}

export interface DashboardView {
  activeChildren: number
  pendingRequests: number
  openRequests: number
  activeVolunteers: number
  activeAssignments: number
  completedAssignments: number
}

export interface PublicAidView {
  id: number
  requestNo: string
  ageGroup: string
  region: string
  category: string
  title: string
  summary: string
  priority: string
  createdAt: string
}

export interface ChildView {
  id: number
  fileNo: string
  departmentId: number
  departmentName: string
  name: string
  gender: string
  birthDate: string
  region: string
  schoolStage: string
  guardianName: string
  guardianPhone: string
  address: string
  familySummary: string
  riskLevel: string
  status: string
  rejectionReason?: string
  createdBy: number
  creatorName: string
  version: number
  createdAt: string
  updatedAt: string
}

export interface AidView {
  id: number
  requestNo: string
  childId: number
  childFileNo: string
  childName: string
  departmentId: number
  departmentName: string
  category: string
  title: string
  description: string
  publicSummary: string
  priority: string
  status: string
  rejectionReason?: string
  createdBy: number
  creatorName: string
  version: number
  createdAt: string
  updatedAt: string
}

export interface ApplicationView {
  id: number
  requestId: number
  requestNo: string
  requestTitle: string
  volunteerId: number
  volunteerName: string
  message: string
  status: string
  decidedAt?: string
  createdAt: string
}

export interface VisitView {
  id: number
  serviceDate: string
  durationMinutes: number
  content: string
  result: string
  creatorName: string
  createdAt: string
}

export interface AssignmentView {
  id: number
  requestId: number
  requestNo: string
  requestTitle: string
  category: string
  volunteerId: number
  volunteerName: string
  departmentId: number
  childFileNo: string
  childName: string
  guardianName: string
  guardianPhone: string
  address: string
  region: string
  status: string
  startedAt?: string
  completionSummary?: string
  submittedAt?: string
  completedAt?: string
  version: number
  requestVersion: number
  createdAt: string
  visits: VisitView[]
}

export interface VolunteerView {
  userId: number
  username: string
  displayName: string
  realName?: string
  phone?: string
  serviceRegion?: string
  skills?: string
  availableTime?: string
  introduction?: string
  certificationStatus: string
  rejectionReason?: string
  updatedAt: string
}

export interface UserAdminView {
  id: number
  username: string
  displayName: string
  roleCode: RoleCode
  roleName: string
  departmentId?: number
  departmentName?: string
  status: string
  lastLoginAt?: string
  createdAt: string
}

export interface DepartmentView {
  id: number
  code: string
  name: string
  enabled: boolean
  createdAt: string
}

export interface PermissionView {
  id: number
  code: string
  name: string
  module: string
}

export interface RoleView {
  id: number
  code: RoleCode
  name: string
  dataScope: string
  permissionIds: number[]
}

export interface AuditView {
  id: number
  userId?: number
  username?: string
  action: string
  businessType: string
  businessId?: string
  beforeStatus?: string
  afterStatus?: string
  detail?: string
  ipAddress?: string
  createdAt: string
}
