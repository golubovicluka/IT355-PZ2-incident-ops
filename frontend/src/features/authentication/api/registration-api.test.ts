import { beforeEach, describe, expect, it, vi } from "vitest"

import { registerAccount } from "@/features/authentication/api/registration-api"

const apiClientMocks = vi.hoisted(() => ({
  post: vi.fn(),
}))

vi.mock("@/shared/api/api-client", () => ({
  apiClient: apiClientMocks,
}))

describe("registration API", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("posts the account details to the public registration endpoint", () => {
    const request = {
      displayName: "New Response Engineer",
      username: "new.responder",
      password: "strong-password",
    }

    registerAccount(request)

    expect(apiClientMocks.post).toHaveBeenCalledWith("/register", request)
  })
})
