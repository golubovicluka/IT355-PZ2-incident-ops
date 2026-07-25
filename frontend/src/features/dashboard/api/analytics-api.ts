import type { OperationalSummary } from "@/features/dashboard/model/analytics.types"
import { apiClient } from "@/shared/api/api-client"

export function getOperationalSummary(signal?: AbortSignal) {
  return apiClient.get<OperationalSummary>("/api/analytics/summary", {
    signal,
  })
}
