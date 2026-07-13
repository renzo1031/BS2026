export type AvatarReviewState = 'none' | 'pending' | 'approved' | 'rejected'

export function avatarReviewState(status?: string): AvatarReviewState {
  if (status === 'APPROVED') return 'approved'
  if (status === 'REJECTED' || status === 'DELETED') return 'rejected'
  return status ? 'pending' : 'none'
}
