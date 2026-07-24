import type {
  Team,
  TeamNameRequest,
} from "@/features/administration/model/team.types"
import { apiClient } from "@/shared/api/api-client"

const TEAMS_PATH = "/api/admin/teams"

export function listTeams(signal?: AbortSignal) {
  return apiClient.get<Team[]>(TEAMS_PATH, { signal })
}

export function createTeam(request: TeamNameRequest) {
  return apiClient.post<Team>(TEAMS_PATH, request)
}

export function updateTeam(id: number, request: TeamNameRequest) {
  return apiClient.put<Team>(`${TEAMS_PATH}/${id}`, request)
}

export function deleteTeam(id: number) {
  return apiClient.delete<void>(`${TEAMS_PATH}/${id}`)
}
