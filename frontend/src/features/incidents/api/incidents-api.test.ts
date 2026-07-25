import { beforeEach, describe, expect, it, vi } from "vitest"

import {
  addIncidentNote,
  createIncident,
  deleteIncident,
  escalateIncident,
  getIncident,
  listIncidents,
  transitionIncidentStatus,
  updateIncident,
} from "@/features/incidents/api/incidents-api"

const apiClientMocks = vi.hoisted(() => ({
  delete: vi.fn(),
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
    transitionIncidentStatus(42, "ACKNOWLEDGED")
    addIncidentNote(42, "Rolled back the checkout deployment.")
    escalateIncident(42, "Checkout is unavailable.")
    deleteIncident(42)

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
    expect(apiClientMocks.put).toHaveBeenCalledWith(
      "/api/incidents/42/status",
      { status: "ACKNOWLEDGED" },
    )
    expect(apiClientMocks.post).toHaveBeenCalledWith(
      "/api/incidents/42/events",
      { note: "Rolled back the checkout deployment." },
    )
    expect(apiClientMocks.post).toHaveBeenCalledWith(
      "/api/incidents/42/escalations",
      { reason: "Checkout is unavailable." },
    )
    expect(apiClientMocks.delete).toHaveBeenCalledWith("/api/incidents/42")
  })
})
