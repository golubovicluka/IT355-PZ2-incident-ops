import type {
  IncidentDetail,
  IncidentFilters,
  IncidentRequest,
  IncidentStatus,
  IncidentSummary,
} from "@/features/incidents/model/incident.types"
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

export function getIncident(id: number, signal?: AbortSignal) {
  return apiClient.get<IncidentDetail>(`${INCIDENTS_PATH}/${id}`, {
    signal,
  })
}

export function createIncident(request: IncidentRequest) {
  return apiClient.post<IncidentDetail>(INCIDENTS_PATH, request)
}

export function updateIncident(id: number, request: IncidentRequest) {
  return apiClient.put<IncidentDetail>(`${INCIDENTS_PATH}/${id}`, request)
}

export function transitionIncidentStatus(
  id: number,
  status: IncidentStatus,
) {
  return apiClient.put<IncidentDetail>(`${INCIDENTS_PATH}/${id}/status`, {
    status,
  })
}

export function addIncidentNote(id: number, note: string) {
  return apiClient.post<IncidentDetail>(`${INCIDENTS_PATH}/${id}/events`, {
    note,
  })
}

export function escalateIncident(id: number, reason: string) {
  return apiClient.post<IncidentDetail>(
    `${INCIDENTS_PATH}/${id}/escalations`,
    { reason },
  )
}

export function deleteIncident(id: number) {
  return apiClient.delete<void>(`${INCIDENTS_PATH}/${id}`)
}
