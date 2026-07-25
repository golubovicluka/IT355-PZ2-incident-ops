import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { beforeEach, describe, expect, it, vi } from "vitest"

import {
  deleteEscalationPolicy,
  listEscalationPolicies,
} from "@/features/administration/api/policies-api"
import { listManagedServices } from "@/features/administration/api/services-api"
import { PolicyManagementSection } from "@/features/administration/components/PolicyManagementSection"
import { notifyManagedServicesChanged } from "@/features/administration/model/administration.events"
import type { EscalationPolicy } from "@/features/administration/model/policy.types"
import type { ManagedService } from "@/features/administration/model/service.types"

vi.mock("@/features/administration/api/policies-api", () => ({
  createEscalationPolicy: vi.fn(),
  deleteEscalationPolicy: vi.fn(),
  listEscalationPolicies: vi.fn(),
  updateEscalationPolicy: vi.fn(),
}))

vi.mock("@/features/administration/api/services-api", () => ({
  listManagedServices: vi.fn(),
}))

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

const mockListPolicies = vi.mocked(listEscalationPolicies)
const mockListServices = vi.mocked(listManagedServices)
const mockDeletePolicy = vi.mocked(deleteEscalationPolicy)

describe("PolicyManagementSection", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockListPolicies.mockResolvedValue([])
    mockListServices.mockResolvedValue([service])
    mockDeletePolicy.mockResolvedValue(undefined)
  })

  it("renders loading and empty states from the API result", async () => {
    render(<PolicyManagementSection />)

    expect(
      screen.getByRole("status", { name: "Loading escalation policies" }),
    ).toBeInTheDocument()
    expect(
      await screen.findByText("No escalation policies yet"),
    ).toBeInTheDocument()
    expect(screen.getAllByRole("button", { name: "Create policy" })).toHaveLength(
      2,
    )
  })

  it("shows a retryable error without inventing policy data", async () => {
    const user = userEvent.setup()
    mockListPolicies.mockRejectedValueOnce(new Error("offline"))

    render(<PolicyManagementSection />)

    expect(
      await screen.findByText("Unable to load escalation policies"),
    ).toBeInTheDocument()
    expect(screen.queryByText(service.name)).not.toBeInTheDocument()

    await user.click(screen.getByRole("button", { name: "Retry" }))

    expect(
      await screen.findByText("No escalation policies yet"),
    ).toBeInTheDocument()
    expect(mockListPolicies).toHaveBeenCalledTimes(2)
  })

  it("reloads policies and services after the catalog changes", async () => {
    mockListServices.mockResolvedValueOnce([])
    render(<PolicyManagementSection />)

    expect(
      await screen.findByText("Create a managed service first"),
    ).toBeInTheDocument()

    mockListPolicies.mockResolvedValue([policy])
    mockListServices.mockResolvedValue([service])
    notifyManagedServicesChanged()

    expect(await screen.findByText(service.name)).toBeInTheDocument()
    expect(screen.getByText("10 min")).toBeInTheDocument()
    expect(
      screen.getByRole("button", { name: "Create policy" }),
    ).toBeEnabled()
  })

  it("deletes a policy only after explicit confirmation", async () => {
    const user = userEvent.setup()
    mockListPolicies.mockResolvedValue([policy])
    render(<PolicyManagementSection />)

    await user.click(
      await screen.findByRole("button", {
        name: "Delete Payments API SEV1 policy",
      }),
    )
    expect(
      screen.getByRole("alertdialog", {
        name: "Delete Payments API SEV1 policy?",
      }),
    ).toBeInTheDocument()

    await user.click(screen.getByRole("button", { name: "Delete policy" }))

    await waitFor(() => expect(mockDeletePolicy).toHaveBeenCalledWith(42))
    expect(
      await screen.findByText("Escalation policy deleted."),
    ).toBeInTheDocument()
    expect(screen.queryByText("10 min")).not.toBeInTheDocument()
  })
})
