import { beforeEach, describe, expect, it, vi } from "vitest"

import { getOperationalSummary } from "@/features/dashboard/api/analytics-api"

const apiClientMocks = vi.hoisted(() => ({
  get: vi.fn(),
}))

vi.mock("@/shared/api/api-client", () => ({
  apiClient: apiClientMocks,
}))

describe("analytics API", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("requests the operational summary with the supplied abort signal", () => {
    const controller = new AbortController()

    getOperationalSummary(controller.signal)

    expect(apiClientMocks.get).toHaveBeenCalledWith(
      "/api/analytics/summary",
      { signal: controller.signal },
    )
  })
})
