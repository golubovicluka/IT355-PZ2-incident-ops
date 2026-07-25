import { render, screen, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { MemoryRouter, useLocation } from "react-router-dom"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { DashboardPage } from "@/features/dashboard/pages/DashboardPage"
import { getOperationalSummary } from "@/features/dashboard/api/analytics-api"
import {
  listIncidents,
  type IncidentDetail,
  type IncidentSummary,
} from "@/features/incidents"
import { listServiceCatalog } from "@/shared/catalogs/catalog-api"
import type { ManagedServiceCatalogItem } from "@/shared/catalogs/catalog.types"

const authenticationMocks = vi.hoisted(() => ({
  hasRole: vi.fn(),
}))

vi.mock("@/features/authentication", () => ({
  useAuthentication: () => ({
    hasRole: authenticationMocks.hasRole,
  }),
}))

vi.mock("@/features/incidents", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/features/incidents")>()),
  IncidentDetailPanel: ({
    incidentId,
    initialIncident,
    onUpdated,
    canDelete,
    onDeleted,
  }: {
    incidentId: number
    initialIncident?: IncidentDetail
    onUpdated?: (incident: IncidentDetail) => void
    canDelete?: boolean
    onDeleted?: (incident: IncidentDetail) => void
  }) => (
    <div>
      Incident detail {incidentId}
      {initialIncident ? `: ${initialIncident.title}` : ""}
      <button
        onClick={() => onUpdated?.(acknowledgedIncident)}
        type="button"
      >
        Complete incident update
      </button>
      {canDelete ? (
        <button
          onClick={() => onDeleted?.(selectedIncidentDetail)}
          type="button"
        >
          Delete selected incident
        </button>
      ) : null}
    </div>
  ),
  IncidentFormDialog: ({
    onOpenChange,
    onSuccess,
    open,
  }: {
    onOpenChange: (open: boolean) => void
    onSuccess: (incident: IncidentDetail) => void
    open: boolean
  }) =>
    open ? (
      <button
        onClick={() => {
          onSuccess(createdIncident)
          onOpenChange(false)
        }}
        type="button"
      >
        Complete incident report
      </button>
    ) : null,
  listIncidents: vi.fn(),
}))

vi.mock("@/shared/catalogs/catalog-api", () => ({
  listServiceCatalog: vi.fn(),
}))

vi.mock("@/features/dashboard/api/analytics-api", () => ({
  getOperationalSummary: vi.fn(),
}))

const mockListIncidents = vi.mocked(listIncidents)
const mockListServices = vi.mocked(listServiceCatalog)
const mockGetOperationalSummary = vi.mocked(getOperationalSummary)

const service: ManagedServiceCatalogItem = {
  id: 7,
  name: "Payments API",
  description: "Processes card payments.",
  criticality: "CRITICAL",
  owningTeam: {
    id: 3,
    name: "Platform Operations",
  },
}

const incident: IncidentSummary = {
  id: 42,
  referenceCode: "INC-20260725-AB12CD34",
  title: "Checkout failures",
  priority: "SEV1",
  status: "OPEN",
  managedService: {
    id: service.id,
    name: service.name,
  },
  assignee: {
    id: 12,
    username: "ana",
    displayName: "Ana Anić",
  },
  sla: {
    state: "BREACHED",
    phase: "ACKNOWLEDGEMENT",
    deadline: "2026-07-25T08:20:30Z",
  },
  createdAt: "2026-07-25T08:15:30Z",
  updatedAt: "2026-07-25T08:15:30Z",
}

const createdIncident: IncidentDetail = {
  ...incident,
  id: 99,
  referenceCode: "INC-20260725-NEWINC99",
  title: "New checkout incident",
  description: "Checkout requests fail before payment authorization.",
  reporter: {
    id: 11,
    username: "luka",
    displayName: "Luka Golubović",
  },
  acknowledgedAt: null,
  resolvedAt: null,
  allowedTransitions: ["ACKNOWLEDGED", "INVESTIGATING"],
  escalations: [],
  timeline: [
    {
      id: 100,
      kind: "CREATED",
      actor: {
        id: 11,
        username: "luka",
        displayName: "Luka Golubović",
      },
      previousStatus: null,
      newStatus: null,
      note: null,
      occurredAt: "2026-07-25T09:00:00Z",
    },
  ],
}

const selectedIncidentDetail: IncidentDetail = {
  ...createdIncident,
  ...incident,
  description: "Card payments are timing out.",
  reporter: createdIncident.reporter,
  acknowledgedAt: null,
  resolvedAt: null,
  allowedTransitions: ["ACKNOWLEDGED", "INVESTIGATING"],
  escalations: [],
  timeline: createdIncident.timeline,
}

const acknowledgedIncident: IncidentDetail = {
  ...selectedIncidentDetail,
  status: "ACKNOWLEDGED",
  acknowledgedAt: "2026-07-25T08:18:30Z",
  allowedTransitions: ["INVESTIGATING"],
  sla: {
    state: "ON_TRACK",
    phase: "RESOLUTION",
    deadline: "2026-07-25T09:15:30Z",
  },
}

function LocationProbe() {
  const location = useLocation()
  return <output data-testid="location-search">{location.search}</output>
}

function renderDashboard(initialEntry = "/dashboard") {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <DashboardPage />
      <LocationProbe />
    </MemoryRouter>,
  )
}

describe("DashboardPage", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockListServices.mockResolvedValue([])
    authenticationMocks.hasRole.mockReturnValue(false)
    mockGetOperationalSummary.mockResolvedValue({
      open: 3,
      active: 2,
      resolved: 5,
      breached: 1,
    })
  })

  it("removes a deleted incident from the administrator queue and updates feedback", async () => {
    const user = userEvent.setup()
    authenticationMocks.hasRole.mockImplementation(
      (role) => role === "ADMIN",
    )
    mockListIncidents.mockResolvedValue([incident])

    renderDashboard()

    await user.click(
      await screen.findByRole("button", {
        name: new RegExp(incident.referenceCode),
      }),
    )
    await user.click(
      screen.getByRole("button", { name: "Delete selected incident" }),
    )

    expect(screen.queryByText(incident.referenceCode)).not.toBeInTheDocument()
    expect(
      screen.getByText(`${incident.referenceCode} was deleted.`),
    ).toBeInTheDocument()
    expect(
      screen.queryByText("Incident detail 42"),
    ).not.toBeInTheDocument()
  })

  it("shows server-calculated counters and incident SLA deadlines", async () => {
    mockListIncidents.mockResolvedValue([incident])

    renderDashboard()

    expect(await screen.findByText(incident.referenceCode)).toBeInTheDocument()
    const summary = within(
      screen.getByRole("region", { name: "Operational summary" }),
    )
    expect(summary.getByText("Open")).toBeInTheDocument()
    expect(summary.getByText("3")).toBeInTheDocument()
    expect(summary.getByText("Active")).toBeInTheDocument()
    expect(summary.getByText("2")).toBeInTheDocument()
    expect(summary.getByText("Resolved")).toBeInTheDocument()
    expect(summary.getByText("5")).toBeInTheDocument()
    expect(summary.getByText("SLA breached")).toBeInTheDocument()
    expect(screen.getByText("Acknowledgement breached")).toBeInTheDocument()
    expect(
      screen.getByText(/Deadline/, { selector: "span" }),
    ).toBeInTheDocument()
  })

  it("distinguishes initial loading from an empty incident queue", async () => {
    mockListIncidents.mockResolvedValue([])

    renderDashboard()

    expect(
      screen.getByRole("status", { name: "Loading incident queue" }),
    ).toBeInTheDocument()
    expect(
      await screen.findByText("No incidents in the queue"),
    ).toBeInTheDocument()
  })

  it("shows a retryable queue error without inventing incident data", async () => {
    const user = userEvent.setup()
    mockListIncidents
      .mockRejectedValueOnce(new Error("offline"))
      .mockResolvedValueOnce([])

    renderDashboard()

    expect(
      await screen.findByText("Unable to load incident queue"),
    ).toBeInTheDocument()
    expect(screen.queryByText("INC-20260725-AB12CD34")).not.toBeInTheDocument()

    await user.click(screen.getByRole("button", { name: "Retry" }))

    expect(
      await screen.findByText("No incidents in the queue"),
    ).toBeInTheDocument()
    expect(mockListIncidents).toHaveBeenCalledTimes(2)
  })

  it("keeps previously loaded dashboard data visible when a refresh fails", async () => {
    const user = userEvent.setup()
    mockListIncidents
      .mockResolvedValueOnce([incident])
      .mockRejectedValueOnce(new Error("offline"))

    renderDashboard()
    await screen.findByText(incident.referenceCode)

    await user.click(screen.getByRole("combobox", { name: "Status" }))
    await user.click(screen.getByRole("option", { name: "Investigating" }))

    expect(
      await screen.findByText("Dashboard data may be stale"),
    ).toBeInTheDocument()
    expect(screen.getByText(incident.referenceCode)).toBeInTheDocument()
    expect(screen.getByText("SLA breached")).toBeInTheDocument()
  })

  it("loads incident summaries using filters from the dashboard URL", async () => {
    mockListServices.mockResolvedValue([service])
    mockListIncidents.mockResolvedValue([incident])

    renderDashboard(
      "/dashboard?status=OPEN&priority=SEV1&serviceId=7",
    )

    expect(await screen.findByText(incident.referenceCode)).toBeInTheDocument()
    expect(screen.getByText(incident.title)).toBeInTheDocument()
    expect(screen.getAllByText(service.name).length).toBeGreaterThan(0)
    expect(screen.getAllByText("Ana Anić").length).toBeGreaterThan(0)
    expect(mockListIncidents).toHaveBeenCalledWith(
      {
        status: "OPEN",
        priority: "SEV1",
        serviceId: 7,
      },
      expect.any(AbortSignal),
    )
  })

  it("writes filter changes to the URL and replaces the API result", async () => {
    const user = userEvent.setup()
    mockListServices.mockResolvedValue([service])
    mockListIncidents
      .mockResolvedValueOnce([incident])
      .mockResolvedValueOnce([])

    renderDashboard()
    await screen.findByText(incident.referenceCode)

    await user.click(screen.getByRole("combobox", { name: "Status" }))
    await user.click(screen.getByRole("option", { name: "Investigating" }))

    expect(screen.getByTestId("location-search")).toHaveTextContent(
      "?status=INVESTIGATING",
    )
    expect(mockListIncidents).toHaveBeenLastCalledWith(
      {
        status: "INVESTIGATING",
        priority: undefined,
        serviceId: undefined,
      },
      expect.any(AbortSignal),
    )
    expect(
      await screen.findByText("No incidents match these filters"),
    ).toBeInTheDocument()
    expect(screen.queryByText(incident.referenceCode)).not.toBeInTheDocument()
  })

  it("marks the chosen incident summary as selected", async () => {
    const user = userEvent.setup()
    mockListServices.mockResolvedValue([service])
    mockListIncidents.mockResolvedValue([incident])

    renderDashboard()

    const summary = await screen.findByRole("button", {
      name: new RegExp(incident.referenceCode),
    })
    expect(summary).toHaveAttribute("aria-pressed", "false")

    await user.click(summary)

    expect(summary).toHaveAttribute("aria-pressed", "true")
    expect(screen.getByText("Selected")).toBeInTheDocument()
    expect(screen.getByText("Incident detail 42")).toBeInTheDocument()
  })

  it("renders the server-returned incident after a successful report", async () => {
    const user = userEvent.setup()
    mockListIncidents.mockResolvedValue([])

    renderDashboard()
    await screen.findByText("No incidents in the queue")

    await user.click(
      screen.getByRole("button", { name: "Report incident" }),
    )
    await user.click(
      screen.getByRole("button", { name: "Complete incident report" }),
    )

    expect(
      screen.getByText("Incident detail 99: New checkout incident"),
    ).toBeInTheDocument()
    expect(screen.getByText(createdIncident.referenceCode)).toBeInTheDocument()
    expect(
      screen.getByText(
        "INC-20260725-NEWINC99 was reported successfully.",
      ),
    ).toBeInTheDocument()
  })

  it("refreshes server counters after created and updated mutation responses", async () => {
    const user = userEvent.setup()
    mockListIncidents.mockResolvedValue([])
    mockGetOperationalSummary
      .mockResolvedValueOnce({
        open: 3,
        active: 2,
        resolved: 5,
        breached: 1,
      })
      .mockResolvedValueOnce({
        open: 4,
        active: 2,
        resolved: 5,
        breached: 2,
      })
      .mockResolvedValueOnce({
        open: 3,
        active: 3,
        resolved: 5,
        breached: 1,
      })

    renderDashboard()
    await screen.findByText("No incidents in the queue")

    await user.click(
      screen.getByRole("button", { name: "Report incident" }),
    )
    await user.click(
      screen.getByRole("button", { name: "Complete incident report" }),
    )

    const summary = within(
      screen.getByRole("region", { name: "Operational summary" }),
    )
    expect(await summary.findByText("4")).toBeInTheDocument()

    await user.click(
      screen.getByRole("button", { name: "Complete incident update" }),
    )

    expect(await summary.findAllByText("3")).toHaveLength(2)
    expect(mockGetOperationalSummary).toHaveBeenCalledTimes(3)
  })
})
