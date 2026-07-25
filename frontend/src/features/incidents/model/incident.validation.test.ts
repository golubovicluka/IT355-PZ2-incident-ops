import { describe, expect, it } from "vitest"

import {
  normalizeIncidentForm,
  validateIncidentForm,
} from "@/features/incidents/model/incident.validation"

describe("incident form validation", () => {
  it("shares required-field validation for create and edit submissions", () => {
    expect(
      validateIncidentForm({
        title: " ",
        description: "",
        priority: null,
        managedServiceId: null,
        assigneeId: null,
      }),
    ).toEqual({
      title: "Incident title is required",
      description: "Incident description is required",
      priority: "Incident priority is required",
      managedServiceId: "Managed service must be selected",
    })
  })

  it("normalizes text without changing optional assignment", () => {
    expect(
      normalizeIncidentForm({
        title: "  Checkout failures ",
        description: "  Card payments are timing out.  ",
        priority: "SEV1",
        managedServiceId: 7,
        assigneeId: null,
      }),
    ).toEqual({
      title: "Checkout failures",
      description: "Card payments are timing out.",
      priority: "SEV1",
      managedServiceId: 7,
      assigneeId: null,
    })
  })

  it("matches the backend length limits", () => {
    expect(
      validateIncidentForm({
        title: "a".repeat(201),
        description: "b".repeat(4001),
        priority: "SEV2",
        managedServiceId: 7,
        assigneeId: 12,
      }),
    ).toEqual({
      title: "Incident title must not exceed 200 characters",
      description: "Incident description must not exceed 4000 characters",
    })
  })
})
