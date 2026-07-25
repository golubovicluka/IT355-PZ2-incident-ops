export type IncidentPriority = "SEV1" | "SEV2" | "SEV3" | "SEV4"

export interface EscalationPolicy {
  id: number
  managedService: {
    id: number
    name: string
  }
  priority: IncidentPriority
  acknowledgementMinutes: number
  resolutionMinutes: number
}

export interface EscalationPolicyRequest {
  managedServiceId: number
  priority: IncidentPriority
  acknowledgementMinutes: number
  resolutionMinutes: number
}
