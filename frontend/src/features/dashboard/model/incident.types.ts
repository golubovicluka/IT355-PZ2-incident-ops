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

export type IncidentStatus = (typeof incidentStatuses)[number]
export type IncidentPriority = (typeof incidentPriorities)[number]

export interface IncidentFilters {
  status?: IncidentStatus
  priority?: IncidentPriority
  serviceId?: number
}

export interface IncidentSummary {
  id: number
  referenceCode: string
  title: string
  priority: IncidentPriority
  status: IncidentStatus
  managedService: {
    id: number
    name: string
  }
  assignee: {
    id: number
    username: string
    displayName: string
  } | null
  createdAt: string
  updatedAt: string
}
