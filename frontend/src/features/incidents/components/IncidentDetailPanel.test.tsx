import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { beforeEach, describe, expect, it, vi } from "vitest"

import {
  getIncident,
  updateIncident,
} from "@/features/incidents/api/incidents-api"
import { IncidentDetailPanel } from "@/features/incidents/components/IncidentDetailPanel"
import type { IncidentDetail } from "@/features/incidents/model/incident.types"
import { ApiError } from "@/shared/api/api-error"
import {
  listAssignableUserCatalog,
  listServiceCatalog,
} from "@/shared/catalogs/catalog-api"

vi.mock("@/features/incidents/api/incidents-api", () => ({
  getIncident: vi.fn(),
  updateIncident: vi.fn(),
}))

vi.mock("@/shared/catalogs/catalog-api", () => ({
  listAssignableUserCatalog: vi.fn(),
  listServiceCatalog: vi.fn(),
}))

const mockGetIncident = vi.mocked(getIncident)
const mockUpdateIncident = vi.mocked(updateIncident)
const mockListServices = vi.mocked(listServiceCatalog)
const mockListUsers = vi.mocked(listAssignableUserCatalog)

const incident: IncidentDetail = {
  id: 42,
  referenceCode: "INC-20260725-AB12CD34",
  title: "Checkout failures",
  description: "Card payments are timing out.",
  priority: "SEV1",
  status: "OPEN",
  managedService: {
    id: 7,
    name: "Payments API",
  },
  reporter: {
    id: 11,
    username: "luka",
    displayName: "Luka Golubović",
  },
  assignee: {
    id: 12,
    username: "ana",
    displayName: "Ana Anić",
  },
  createdAt: "2026-07-25T08:15:30Z",
  updatedAt: "2026-07-25T08:15:30Z",
  timeline: [
    {
      id: 91,
      kind: "CREATED",
      actor: {
        id: 11,
        username: "luka",
        displayName: "Luka Golubović",
      },
      occurredAt: "2026-07-25T08:15:30Z",
    },
  ],
}

describe("IncidentDetailPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockListServices.mockResolvedValue([
      {
        id: 7,
        name: "Payments API",
        description: "Processes card payments.",
        criticality: "CRITICAL",
        owningTeam: {
          id: 3,
          name: "Platform Operations",
        },
      },
    ])
    mockListUsers.mockResolvedValue([
      {
        id: 12,
        username: "ana",
        displayName: "Ana Anić",
        team: {
          id: 3,
          name: "Platform Operations",
        },
      },
    ])
  })

  it("loads a selected incident through the detail endpoint", async () => {
    mockGetIncident.mockResolvedValue(incident)

    render(
      <IncidentDetailPanel
        incidentId={incident.id}
        onUpdated={vi.fn()}
      />,
    )

    expect(
      screen.getByRole("status", { name: "Loading incident detail" }),
    ).toBeInTheDocument()
    expect(
      await screen.findByRole("heading", { name: incident.title }),
    ).toBeInTheDocument()
    expect(mockGetIncident).toHaveBeenCalledWith(
      incident.id,
      expect.any(AbortSignal),
    )
  })

  it("renders the server-returned detail and initial created timeline entry", () => {
    render(
      <IncidentDetailPanel
        incidentId={incident.id}
        initialIncident={incident}
        onUpdated={vi.fn()}
      />,
    )

    expect(
      screen.getByRole("heading", { name: incident.title }),
    ).toBeInTheDocument()
    expect(screen.getByText(incident.description)).toBeInTheDocument()
    expect(screen.getByText("Luka Golubović")).toBeInTheDocument()
    expect(screen.getByText("Ana Anić")).toBeInTheDocument()
    expect(screen.getByText("Incident created")).toBeInTheDocument()
    expect(
      screen.getByText("Created by Luka Golubović"),
    ).toBeInTheDocument()
    expect(mockGetIncident).not.toHaveBeenCalled()
  })

  it("keeps the loaded detail and edited values after an update fails", async () => {
    const user = userEvent.setup()
    mockUpdateIncident.mockRejectedValue(
      new ApiError({
        timestamp: "2026-07-25T12:00:00Z",
        status: 409,
        error: "Conflict",
        message: "The incident changed while you were editing it",
        path: "/api/incidents/42",
        fieldErrors: {},
      }),
    )

    render(
      <IncidentDetailPanel
        incidentId={incident.id}
        initialIncident={incident}
        onUpdated={vi.fn()}
      />,
    )

    await user.click(screen.getByRole("button", { name: "Edit incident" }))
    const title = await screen.findByLabelText("Title")
    await user.clear(title)
    await user.type(title, "Checkout failures across Europe")
    await user.click(screen.getByRole("button", { name: "Save changes" }))

    expect(
      await screen.findByText(
        "The incident changed while you were editing it",
      ),
    ).toBeInTheDocument()
    expect(title).toHaveValue("Checkout failures across Europe")
    expect(screen.getByText(incident.title)).toBeInTheDocument()
    expect(screen.getByText("Incident created")).toBeInTheDocument()
  })
})
