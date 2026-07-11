export interface ChildFormPayload {
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
  version?: number
}

export interface AidFormPayload {
  childId: number
  category: string
  title: string
  description: string
  publicSummary: string
  priority: string
  version?: number
}

export interface ReviewPayload {
  decision: 'APPROVED' | 'REJECTED'
  comment?: string
  version?: number
}
