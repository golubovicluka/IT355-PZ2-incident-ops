import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { MemoryRouter, useLocation } from "react-router-dom"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { listIncidents } from "@/features/dashboard/api/incidents-api"
import { DashboardPage } from "@/features/dashboard/pages/DashboardPage"
import type { IncidentSummary } from "@/features/dashboard/model/incident.types"
import { listServiceCatalog } from "@/shared/catalogs/catalog-api"
import type { ManagedServiceCatalogItem } from "@/shared/catalogs/catalog.types"

vi.mock("@/features/dashboard/api/incidents-api", () => ({
  listIncidents: vi.fn(),
}))

vi.mock("@/shared/catalogs/catalog-api", () => ({
  listServiceCatalog: vi.fn(),
}))

const mockListIncidents = vi.mocked(listIncidents)
const mockListServices = vi.mocked(listServiceCatalog)

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
  createdAt: "2026-07-25T08:15:30Z",
  updatedAt: "2026-07-25T08:15:30Z",
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
  })
})
