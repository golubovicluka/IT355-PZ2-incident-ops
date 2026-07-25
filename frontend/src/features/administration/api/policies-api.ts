import type {
  EscalationPolicy,
  EscalationPolicyRequest,
} from "@/features/administration/model/policy.types"
import { apiClient } from "@/shared/api/api-client"

const POLICIES_PATH = "/api/admin/policies"

export function listEscalationPolicies(signal?: AbortSignal) {
  return apiClient.get<EscalationPolicy[]>(POLICIES_PATH, { signal })
}

export function createEscalationPolicy(request: EscalationPolicyRequest) {
  return apiClient.post<EscalationPolicy>(POLICIES_PATH, request)
}

export function updateEscalationPolicy(
  id: number,
  request: EscalationPolicyRequest,
) {
  return apiClient.put<EscalationPolicy>(
    `${POLICIES_PATH}/${id}`,
    request,
  )
}

export function deleteEscalationPolicy(id: number) {
  return apiClient.delete<void>(`${POLICIES_PATH}/${id}`)
}
