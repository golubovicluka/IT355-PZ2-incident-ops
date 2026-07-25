import type {
  ManagedService,
  ManagedServiceRequest,
} from "@/features/administration/model/service.types"
import { apiClient } from "@/shared/api/api-client"

const SERVICES_PATH = "/api/admin/services"

export function listManagedServices(signal?: AbortSignal) {
  return apiClient.get<ManagedService[]>(SERVICES_PATH, { signal })
}

export function createManagedService(request: ManagedServiceRequest) {
  return apiClient.post<ManagedService>(SERVICES_PATH, request)
}

export function updateManagedService(
  id: number,
  request: ManagedServiceRequest,
) {
  return apiClient.put<ManagedService>(`${SERVICES_PATH}/${id}`, request)
}

export function deleteManagedService(id: number) {
  return apiClient.delete<void>(`${SERVICES_PATH}/${id}`)
}
