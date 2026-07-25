import { beforeEach, describe, expect, it, vi } from "vitest"

import { listIncidents } from "@/features/dashboard/api/incidents-api"

const apiClientMocks = vi.hoisted(() => ({
  get: vi.fn(),
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
})
