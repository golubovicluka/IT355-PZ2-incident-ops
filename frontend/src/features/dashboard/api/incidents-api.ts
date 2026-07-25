import type {
  IncidentFilters,
  IncidentSummary,
} from "@/features/dashboard/model/incident.types"
import { apiClient } from "@/shared/api/api-client"

const INCIDENTS_PATH = "/api/incidents"

export function listIncidents(
  filters: IncidentFilters = {},
  signal?: AbortSignal,
) {
  const query = new URLSearchParams()

  if (filters.status) {
    query.set("status", filters.status)
  }
  if (filters.priority) {
    query.set("priority", filters.priority)
  }
  if (filters.serviceId) {
    query.set("serviceId", String(filters.serviceId))
  }

  const queryString = query.toString()
  const path = queryString
    ? `${INCIDENTS_PATH}?${queryString}`
    : INCIDENTS_PATH

  return apiClient.get<IncidentSummary[]>(path, { signal })
}
