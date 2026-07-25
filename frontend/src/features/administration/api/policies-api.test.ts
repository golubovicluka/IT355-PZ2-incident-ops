import { beforeEach, describe, expect, it, vi } from "vitest"

import {
  createEscalationPolicy,
  deleteEscalationPolicy,
  listEscalationPolicies,
  updateEscalationPolicy,
} from "@/features/administration/api/policies-api"

const apiClientMocks = vi.hoisted(() => ({
  delete: vi.fn(),
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

vi.mock("@/shared/api/api-client", () => ({
  apiClient: apiClientMocks,
}))

const request = {
  managedServiceId: 7,
  priority: "SEV1" as const,
  acknowledgementMinutes: 10,
  resolutionMinutes: 45,
}

describe("policies API", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("uses the administrator policy endpoints for the complete CRUD boundary", () => {
    const controller = new AbortController()

    listEscalationPolicies(controller.signal)
    createEscalationPolicy(request)
    updateEscalationPolicy(42, request)
    deleteEscalationPolicy(42)

    expect(apiClientMocks.get).toHaveBeenCalledWith("/api/admin/policies", {
      signal: controller.signal,
    })
    expect(apiClientMocks.post).toHaveBeenCalledWith(
      "/api/admin/policies",
      request,
    )
    expect(apiClientMocks.put).toHaveBeenCalledWith(
      "/api/admin/policies/42",
      request,
    )
    expect(apiClientMocks.delete).toHaveBeenCalledWith(
      "/api/admin/policies/42",
    )
  })
})
