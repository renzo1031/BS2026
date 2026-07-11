function labelOf(labels: Record<string, string>, value: string) {
  return labels[value] ?? value
}

export const statusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  ACTIVE: '有效',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  ARCHIVED: '已归档',
  UNVERIFIED: '未认证',
  SUSPENDED: '已暂停',
  MATCHED: '已匹配',
  IN_PROGRESS: '服务中',
  PENDING_ACCEPTANCE: '待验收',
  COMPLETED: '已完成',
  CLOSED: '已结案',
  CANCELLED: '已取消',
  APPLIED: '已申请',
  ACCEPTED: '已接受',
  WITHDRAWN: '已撤回',
  ASSIGNED: '待开始',
  TERMINATED: '已终止',
  DISABLED: '已停用',
}

const genderLabels: Record<string, string> = {
  MALE: '男',
  FEMALE: '女',
  OTHER: '其他',
}

const riskLevelLabels: Record<string, string> = {
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险',
}

const categoryLabels: Record<string, string> = {
  EDUCATION: '学习支持',
  COMPANIONSHIP: '成长陪伴',
  LIFE_CARE: '生活关怀',
  SAFETY: '安全关爱',
  PSYCHOLOGICAL: '心理支持',
  OTHER: '其他支持',
}

const priorityLabels: Record<string, string> = {
  NORMAL: '普通',
  URGENT: '紧急',
}

const businessTypeLabels: Record<string, string> = {
  USER: '用户账号',
  AUTH_SESSION: '登录会话',
  CHILD: '儿童档案',
  AID_REQUEST: '帮扶需求',
  AID_APPLICATION: '志愿申请',
  ASSIGNMENT: '服务任务',
  VOLUNTEER: '志愿者认证',
  ROLE: '角色权限',
  DEPARTMENT: '业务部门',
}

const auditActionLabels: Record<string, string> = {
  CREATE_DEPARTMENT: '创建部门',
  CREATE_USER: '创建账号',
  REVIEW_VOLUNTEER: '审核志愿者',
  UPDATE_ROLE_PERMISSIONS: '更新角色权限',
  UPDATE_USER_STATUS: '更新账号状态',
  APPLY_AID: '申请帮扶需求',
  CANCEL_AID: '取消帮扶需求',
  CREATE_AID: '创建帮扶需求',
  MATCH_VOLUNTEER: '匹配志愿者',
  REVIEW_AID: '审核帮扶需求',
  SUBMIT_AID: '提交需求审核',
  UPDATE_AID: '更新帮扶需求',
  WITHDRAW_APPLICATION: '撤回志愿申请',
  ADD_FEEDBACK: '提交服务评价',
  ADD_VISIT: '新增服务回访',
  CONFIRM_COMPLETION: '验收服务任务',
  START_ASSIGNMENT: '开始服务任务',
  SUBMIT_COMPLETION: '提交完成验收',
  ARCHIVE_CHILD: '归档儿童档案',
  CREATE_CHILD: '创建儿童档案',
  REVIEW_CHILD: '审核儿童档案',
  SUBMIT_CHILD: '提交档案审核',
  UPDATE_CHILD: '更新儿童档案',
  SUBMIT_VOLUNTEER: '提交志愿认证',
  UPDATE_VOLUNTEER: '更新志愿资料',
}

export const statusLabel = (value: string) => labelOf(statusLabels, value)
export const genderLabel = (value: string) => labelOf(genderLabels, value)
export const riskLevelLabel = (value: string) => labelOf(riskLevelLabels, value)
export const categoryLabel = (value: string) => labelOf(categoryLabels, value)
export const priorityLabel = (value: string) => labelOf(priorityLabels, value)
export const businessTypeLabel = (value: string) => labelOf(businessTypeLabels, value)
export const auditActionLabel = (value: string) => labelOf(auditActionLabels, value)
