export const incidentStatuses = [
  "OPEN",
  "ACKNOWLEDGED",
  "INVESTIGATING",
  "RESOLVED",
  "CLOSED",
] as const

export const incidentPriorities = [
  "SEV1",
  "SEV2",
  "SEV3",
  "SEV4",
] as const

export const incidentEventKinds = [
  "CREATED",
  "STATUS_CHANGED",
  "NOTE_ADDED",
  "ESCALATED",
] as const

export type IncidentStatus = (typeof incidentStatuses)[number]
export type IncidentPriority = (typeof incidentPriorities)[number]
export type IncidentEventKind = (typeof incidentEventKinds)[number]
export type SlaState =
  | "NOT_CONFIGURED"
  | "ON_TRACK"
  | "BREACHED"
  | "MET"
export type SlaPhase = "ACKNOWLEDGEMENT" | "RESOLUTION"

export interface IncidentFilters {
  status?: IncidentStatus
  priority?: IncidentPriority
  serviceId?: number
}

export interface IncidentUser {
  id: number
  username: string
  displayName: string
}

export interface IncidentManagedService {
  id: number
  name: string
}

export interface IncidentSla {
  state: SlaState
  phase: SlaPhase | null
  deadline: string | null
}

export interface IncidentSummary {
  id: number
  referenceCode: string
  title: string
  priority: IncidentPriority
  status: IncidentStatus
  managedService: IncidentManagedService
  assignee: IncidentUser | null
  sla: IncidentSla
  createdAt: string
  updatedAt: string
}

export interface IncidentTimelineEntry {
  id: number
  kind: IncidentEventKind
  actor: IncidentUser
  previousStatus: IncidentStatus | null
  newStatus: IncidentStatus | null
  note: string | null
  escalationLevel?: number | null
  escalationReason?: string | null
  occurredAt: string
}

export interface IncidentEscalation {
  level: number
  reason: string
  actor: IncidentUser
  escalatedAt: string
}

export interface IncidentDetail extends IncidentSummary {
  description: string
  reporter: IncidentUser
  acknowledgedAt: string | null
  resolvedAt: string | null
  allowedTransitions: IncidentStatus[]
  timeline: IncidentTimelineEntry[]
  escalations: IncidentEscalation[]
}

export interface IncidentRequest {
  title: string
  description: string
  priority: IncidentPriority
  managedServiceId: number
  assigneeId: number | null
}
