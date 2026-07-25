import { describe, expect, it } from "vitest"

import {
  toPositiveWholeMinutes,
  validatePolicyForm,
} from "@/features/administration/model/policy.validation"

describe("policy form validation", () => {
  it("requires a service, priority, and positive whole-minute deadlines", () => {
    expect(
      validatePolicyForm({
        managedServiceId: null,
        priority: null,
        acknowledgementMinutes: "0",
        resolutionMinutes: "1.5",
      }),
    ).toEqual({
      managedServiceId: "Select a managed service.",
      priority: "Select an incident priority.",
      acknowledgementMinutes:
        "Enter a positive whole-minute acknowledgement deadline.",
      resolutionMinutes:
        "Enter a positive whole-minute resolution deadline.",
    })
  })

  it("rejects acknowledgement deadlines after the resolution deadline", () => {
    expect(
      validatePolicyForm({
        managedServiceId: 7,
        priority: "SEV1",
        acknowledgementMinutes: "60",
        resolutionMinutes: "45",
      }),
    ).toEqual({
      acknowledgementMinutes:
        "Acknowledgement deadline must not exceed the resolution deadline.",
    })
  })

  it("normalizes a valid whole-minute value for the API request", () => {
    expect(toPositiveWholeMinutes(" 45 ")).toBe(45)
    expect(toPositiveWholeMinutes("9007199254740992")).toBeUndefined()
  })
})
