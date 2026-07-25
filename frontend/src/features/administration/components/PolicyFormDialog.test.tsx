import { createRef } from "react"

import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"

import { PolicyFormDialog } from "@/features/administration/components/PolicyFormDialog"
import type { EscalationPolicy } from "@/features/administration/model/policy.types"
import type { ManagedService } from "@/features/administration/model/service.types"
import { ApiError } from "@/shared/api/api-error"

const service: ManagedService = {
  id: 7,
  name: "Payments API",
  description: "Processes card payments.",
  criticality: "CRITICAL",
  owningTeam: {
    id: 3,
    name: "Platform Operations",
  },
}

const policy: EscalationPolicy = {
  id: 42,
  managedService: {
    id: service.id,
    name: service.name,
  },
  priority: "SEV1",
  acknowledgementMinutes: 10,
  resolutionMinutes: 45,
}

describe("PolicyFormDialog", () => {
  it("maps server field errors to the matching control and preserves values", async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockRejectedValue(
      new ApiError({
        timestamp: "2026-07-25T12:00:00Z",
        status: 400,
        error: "Bad Request",
        message: "Request validation failed",
        path: "/api/admin/policies/42",
        fieldErrors: {
          acknowledgementMinutes:
            "Acknowledgement deadline conflicts with the current rule",
        },
      }),
    )

    render(
      <PolicyFormDialog
        finalFocusRef={createRef<HTMLElement>()}
        mode="edit"
        onOpenChange={vi.fn()}
        onSubmit={onSubmit}
        open
        policy={policy}
        services={[service]}
      />,
    )

    const acknowledgement = screen.getByLabelText(
      "Acknowledgement deadline",
    )
    const resolution = screen.getByLabelText("Resolution deadline")
    await user.clear(acknowledgement)
    await user.type(acknowledgement, "20")
    await user.click(screen.getByRole("button", { name: "Save changes" }))

    expect(onSubmit).toHaveBeenCalledWith({
      managedServiceId: 7,
      priority: "SEV1",
      acknowledgementMinutes: 20,
      resolutionMinutes: 45,
    })
    expect(
      await screen.findByText(
        "Acknowledgement deadline conflicts with the current rule",
      ),
    ).toBeInTheDocument()
    expect(acknowledgement).toHaveValue(20)
    expect(resolution).toHaveValue(45)
  })

  it("keeps the form open when the server rejects a duplicate pair", async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockRejectedValue(
      new ApiError({
        timestamp: "2026-07-25T12:00:00Z",
        status: 409,
        error: "Conflict",
        message:
          "An escalation policy already exists for this service and priority",
        path: "/api/admin/policies/42",
        fieldErrors: {},
      }),
    )

    render(
      <PolicyFormDialog
        finalFocusRef={createRef<HTMLElement>()}
        mode="edit"
        onOpenChange={vi.fn()}
        onSubmit={onSubmit}
        open
        policy={policy}
        services={[service]}
      />,
    )

    await user.click(screen.getByRole("button", { name: "Save changes" }))

    expect(
      await screen.findByText(
        "An escalation policy already exists for this service and priority",
      ),
    ).toBeInTheDocument()
    expect(
      screen.getByRole("heading", { name: "Edit escalation policy" }),
    ).toBeInTheDocument()
  })
})
