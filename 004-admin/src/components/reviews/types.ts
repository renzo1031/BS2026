export type ModerationActionType = 'REMOVE_ACTIVITY' | 'LIMIT_USER' | 'SUSPEND_USER'

export const meetingModeLabels: Record<string, string> = {
  ONLINE: '线上',
  OFFLINE: '线下',
  HYBRID: '线上 + 线下',
}

export const identifierTypeLabels: Record<string, string> = {
  STUDENT_ID: '学号',
  STUDENT_NO: '学号',
  CAMPUS_ID: '校园身份编号',
}

export const fileBusinessTypeLabels: Record<string, string> = {
  AVATAR: '用户头像',
  ACTIVITY_IMAGE: '活动图片',
  CHAT_IMAGE: '聊天图片',
  IDENTITY_PROOF: '校园身份证明',
  REPORT_EVIDENCE: '举报证明材料',
}

export const reportReasonLabels: Record<string, string> = {
  HARASSMENT: '人身攻击或骚扰',
  FRAUD: '欺诈或虚假信息',
  INAPPROPRIATE_CONTENT: '不当内容',
  SAFETY_RISK: '安全风险',
  COMPLETION_DISPUTE: '活动完成争议',
  OTHER: '其他违规',
}

export const reportTargetLabels: Record<string, string> = {
  ACTIVITY: '活动',
  USER: '用户',
}

export const activityActionLabels: Record<string, string> = {
  CREATE: '创建活动',
  SUBMIT_REVIEW: '提交审核',
  APPROVE: '审核通过',
  REJECT: '审核驳回',
  START: '开始活动',
  REQUEST_COMPLETION: '申请完成',
  CONFIRM_COMPLETION: '确认完成',
  DISPUTE_COMPLETION: '发起完成争议',
  AUTO_COMPLETE: '系统自动完成',
  CANCEL: '取消活动',
  ACCEPT_APPLICATION: '通过加入申请',
  REJECT_APPLICATION: '驳回加入申请',
  LEAVE_ACTIVITY: '成员退出活动',
  CLAIM_ACTIVITY_REVIEW: '认领活动审核',
  AUTO_RELEASE_ACTIVITY_REVIEW_CLAIM: '系统释放审核认领',
  CLAIM_REVIEW: '认领审核任务',
  SUBMIT_REPORT: '提交举报',
  ACTION_REPORT: '举报处置',
  DISMISS_REPORT: '驳回举报',
  APPEAL_REPORT: '提交申诉',
  UPHOLD_APPEAL: '维持处置',
  REVOKE_ACTION: '撤销处置',
  REMOVE_ACTIVITY: '活动下架',
  RESTORE_ACTIVITY: '恢复活动',
}

export const workflowStatusLabels: Record<string, string> = {
  NOT_SUBMITTED: '未提交',
  DRAFT: '草稿',
  PENDING: '待处理',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  ACCEPTED: '已加入',
  CANCELLED: '已取消',
  WITHDRAWN: '已撤回',
  RECRUITING: '招募中',
  IN_PROGRESS: '进行中',
  COMPLETION_PENDING: '待确认完成',
  COMPLETED: '已完成',
  ACTIVE: '参与中',
  LEFT: '已退出',
  CONFIRMED: '已确认完成',
  DISPUTED: '完成有争议',
  AUTO_CONFIRMED: '系统自动确认',
  CREATOR: '发起人',
  PARTICIPANT: '参与者',
  EXPIRED: '已过期',
  NORMAL: '治理正常',
  UNDER_REVIEW: '治理审查中',
  REMOVED: '已下架',
}
