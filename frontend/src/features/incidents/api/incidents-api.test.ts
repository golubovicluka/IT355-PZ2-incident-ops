import { beforeEach, describe, expect, it, vi } from "vitest"

import {
  createIncident,
  getIncident,
  listIncidents,
  updateIncident,
} from "@/features/incidents/api/incidents-api"

const apiClientMocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

vi.mock("@/shared/api/api-client", () => ({
  apiClient: apiClientMocks,
}))

describe("incidents API", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("sends selected queue filters as incident query parameters", () => {
    const controller = new AbortController()

    listIncidents(
      {
        status: "INVESTIGATING",
        priority: "SEV2",
        serviceId: 7,
      },
      controller.signal,
    )

    expect(apiClientMocks.get).toHaveBeenCalledWith(
      "/api/incidents?status=INVESTIGATING&priority=SEV2&serviceId=7",
      { signal: controller.signal },
    )
  })

  it("requests the unfiltered queue without an empty query string", () => {
    listIncidents()

    expect(apiClientMocks.get).toHaveBeenCalledWith("/api/incidents", {
      signal: undefined,
    })
  })

  it("uses the incident detail and mutation endpoints", () => {
    const controller = new AbortController()
    const request = {
      title: "Checkout failures",
      description: "Card payments are timing out.",
      priority: "SEV1" as const,
      managedServiceId: 7,
      assigneeId: 12,
    }

    getIncident(42, controller.signal)
    createIncident(request)
    updateIncident(42, request)

    expect(apiClientMocks.get).toHaveBeenCalledWith("/api/incidents/42", {
      signal: controller.signal,
    })
    expect(apiClientMocks.post).toHaveBeenCalledWith(
      "/api/incidents",
      request,
    )
    expect(apiClientMocks.put).toHaveBeenCalledWith(
      "/api/incidents/42",
      request,
    )
  })
})
