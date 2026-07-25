import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { beforeEach, describe, expect, it, vi } from "vitest"

import {
  addIncidentNote,
  escalateIncident,
  getIncident,
  transitionIncidentStatus,
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
  addIncidentNote: vi.fn(),
  escalateIncident: vi.fn(),
  getIncident: vi.fn(),
  transitionIncidentStatus: vi.fn(),
  updateIncident: vi.fn(),
}))

vi.mock("@/shared/catalogs/catalog-api", () => ({
  listAssignableUserCatalog: vi.fn(),
  listServiceCatalog: vi.fn(),
}))

const mockGetIncident = vi.mocked(getIncident)
const mockAddIncidentNote = vi.mocked(addIncidentNote)
const mockEscalateIncident = vi.mocked(escalateIncident)
const mockTransitionIncidentStatus = vi.mocked(transitionIncidentStatus)
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
  sla: {
    state: "NOT_CONFIGURED",
    phase: null,
    deadline: null,
  },
  createdAt: "2026-07-25T08:15:30Z",
  updatedAt: "2026-07-25T08:15:30Z",
  acknowledgedAt: null,
  resolvedAt: null,
  allowedTransitions: ["ACKNOWLEDGED", "INVESTIGATING"],
  escalations: [],
  timeline: [
    {
      id: 91,
      kind: "CREATED",
      actor: {
        id: 11,
        username: "luka",
        displayName: "Luka Golubović",
      },
      previousStatus: null,
      newStatus: null,
      note: null,
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
    expect(screen.getByText("SLA not configured")).toBeInTheDocument()
    expect(mockGetIncident).not.toHaveBeenCalled()
  })

  it("shows the breached SLA phase and server-calculated deadline", () => {
    render(
      <IncidentDetailPanel
        incidentId={incident.id}
        initialIncident={{
          ...incident,
          sla: {
            state: "BREACHED",
            phase: "RESOLUTION",
            deadline: "2026-07-25T09:15:30Z",
          },
        }}
        onUpdated={vi.fn()}
      />,
    )

    expect(screen.getByText("Resolution breached")).toBeInTheDocument()
    expect(
      screen.getByText(/Deadline/, { selector: "span" }),
    ).toBeInTheDocument()
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

  it("shows only server-allowed status actions and replaces detail after success", async () => {
    const user = userEvent.setup()
    const transitioned: IncidentDetail = {
      ...incident,
      status: "ACKNOWLEDGED",
      updatedAt: "2026-07-25T08:20:30Z",
      acknowledgedAt: "2026-07-25T08:20:30Z",
      allowedTransitions: ["INVESTIGATING"],
      timeline: [
        ...incident.timeline,
        {
          id: 92,
          kind: "STATUS_CHANGED",
          actor: incident.reporter,
          previousStatus: "OPEN",
          newStatus: "ACKNOWLEDGED",
          note: null,
          occurredAt: "2026-07-25T08:20:30Z",
        },
      ],
    }
    mockTransitionIncidentStatus.mockResolvedValue(transitioned)
    const onUpdated = vi.fn()

    render(
      <IncidentDetailPanel
        incidentId={incident.id}
        initialIncident={incident}
        onUpdated={onUpdated}
      />,
    )

    expect(
      screen.getByRole("button", { name: "Acknowledge incident" }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole("button", { name: "Start investigating" }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Resolve incident" }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Close incident" }),
    ).not.toBeInTheDocument()

    await user.click(
      screen.getByRole("button", { name: "Acknowledge incident" }),
    )

    expect(mockTransitionIncidentStatus).toHaveBeenCalledWith(
      incident.id,
      "ACKNOWLEDGED",
    )
    expect(
      await screen.findByText("Open → Acknowledged"),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Acknowledge incident" }),
    ).not.toBeInTheDocument()
    expect(
      screen.getByRole("button", { name: "Start investigating" }),
    ).toBeInTheDocument()
    expect(onUpdated).toHaveBeenCalledWith(transitioned)
  })

  it("preserves the current status and timeline after a transition conflict", async () => {
    const user = userEvent.setup()
    mockTransitionIncidentStatus.mockRejectedValue(
      new ApiError({
        timestamp: "2026-07-25T12:00:00Z",
        status: 409,
        error: "Conflict",
        message: "Incident status cannot transition from OPEN to CLOSED",
        path: "/api/incidents/42/status",
        fieldErrors: {},
      }),
    )
    const onUpdated = vi.fn()

    render(
      <IncidentDetailPanel
        incidentId={incident.id}
        initialIncident={incident}
        onUpdated={onUpdated}
      />,
    )

    await user.click(
      screen.getByRole("button", { name: "Acknowledge incident" }),
    )

    expect(
      await screen.findByText(
        "Incident status cannot transition from OPEN to CLOSED",
      ),
    ).toBeInTheDocument()
    expect(screen.getByText("Open")).toBeInTheDocument()
    expect(screen.getByText("Incident created")).toBeInTheDocument()
    expect(
      screen.getByRole("button", { name: "Acknowledge incident" }),
    ).toBeInTheDocument()
    expect(onUpdated).not.toHaveBeenCalled()
  })

  it("adds a 2000-character-limited note and clears it only after success", async () => {
    const user = userEvent.setup()
    const note = "Rolled back the checkout deployment."
    const noted: IncidentDetail = {
      ...incident,
      updatedAt: "2026-07-25T08:20:30Z",
      timeline: [
        ...incident.timeline,
        {
          id: 92,
          kind: "NOTE_ADDED",
          actor: incident.reporter,
          previousStatus: null,
          newStatus: null,
          note,
          occurredAt: "2026-07-25T08:20:30Z",
        },
      ],
    }
    mockAddIncidentNote.mockResolvedValue(noted)
    const onUpdated = vi.fn()

    render(
      <IncidentDetailPanel
        incidentId={incident.id}
        initialIncident={incident}
        onUpdated={onUpdated}
      />,
    )

    const noteField = screen.getByLabelText("Timeline note")
    expect(noteField).toHaveAttribute("maxlength", "2000")
    expect(screen.getByText("0 / 2,000 characters")).toBeInTheDocument()

    await user.type(noteField, note)
    expect(
      screen.getByText(`${note.length} / 2,000 characters`),
    ).toBeInTheDocument()
    await user.click(screen.getByRole("button", { name: "Add note" }))

    expect(mockAddIncidentNote).toHaveBeenCalledWith(incident.id, note)
    expect(noteField).toHaveValue("")
    expect(await screen.findByText(note)).toBeInTheDocument()
    expect(
      screen.getByText("Added by Luka Golubović"),
    ).toBeInTheDocument()
    expect(onUpdated).toHaveBeenCalledWith(noted)
  })

  it("validates blank notes and preserves note text after a server failure", async () => {
    const user = userEvent.setup()
    const note = "Waiting for database recovery."
    mockAddIncidentNote.mockRejectedValue(
      new ApiError({
        timestamp: "2026-07-25T12:00:00Z",
        status: 503,
        error: "Service Unavailable",
        message: "The note could not be stored",
        path: "/api/incidents/42/events",
        fieldErrors: {},
      }),
    )
    const onUpdated = vi.fn()

    render(
      <IncidentDetailPanel
        incidentId={incident.id}
        initialIncident={incident}
        onUpdated={onUpdated}
      />,
    )

    await user.click(screen.getByRole("button", { name: "Add note" }))
    expect(
      await screen.findByText("Enter a note before adding it."),
    ).toBeInTheDocument()
    expect(mockAddIncidentNote).not.toHaveBeenCalled()

    const noteField = screen.getByLabelText("Timeline note")
    await user.type(noteField, note)
    await user.click(screen.getByRole("button", { name: "Add note" }))

    expect(
      await screen.findByText("The note could not be stored"),
    ).toBeInTheDocument()
    expect(noteField).toHaveValue(note)
    expect(onUpdated).not.toHaveBeenCalled()
  })

  it("submits a 1000-character-limited escalation and renders server history", async () => {
    const user = userEvent.setup()
    const reason = "Checkout is unavailable for all customers."
    const escalated: IncidentDetail = {
      ...incident,
      updatedAt: "2026-07-25T08:20:30Z",
      escalations: [
        {
          level: 1,
          reason,
          actor: incident.reporter,
          escalatedAt: "2026-07-25T08:20:30Z",
        },
      ],
      timeline: [
        ...incident.timeline,
        {
          id: 92,
          kind: "ESCALATED",
          actor: incident.reporter,
          previousStatus: null,
          newStatus: null,
          note: null,
          escalationLevel: 1,
          escalationReason: reason,
          occurredAt: "2026-07-25T08:20:30Z",
        },
      ],
    }
    mockEscalateIncident.mockResolvedValue(escalated)
    const onUpdated = vi.fn()

    render(
      <IncidentDetailPanel
        incidentId={incident.id}
        initialIncident={incident}
        onUpdated={onUpdated}
      />,
    )

    expect(
      screen.getByText("No manual escalations have been recorded."),
    ).toBeInTheDocument()
    const reasonField = screen.getByLabelText("Escalation reason")
    expect(reasonField).toHaveAttribute("maxlength", "1000")
    expect(screen.getByText("0 / 1,000 characters")).toBeInTheDocument()

    await user.type(reasonField, reason)
    await user.click(
      screen.getByRole("button", { name: "Escalate incident" }),
    )

    expect(mockEscalateIncident).toHaveBeenCalledWith(incident.id, reason)
    expect(reasonField).toHaveValue("")
    expect(await screen.findByText("Level 1")).toBeInTheDocument()
    expect(screen.getAllByText(reason)).toHaveLength(1)
    expect(
      screen.getByText("Escalated by Luka Golubović"),
    ).toBeInTheDocument()
    expect(
      screen.getByText(`Level 1: ${reason}`),
    ).toBeInTheDocument()
    expect(onUpdated).toHaveBeenCalledWith(escalated)
  })

  it("validates blank escalation and preserves its reason after failure", async () => {
    const user = userEvent.setup()
    const reason = "Checkout is unavailable in Europe."
    mockEscalateIncident.mockRejectedValue(
      new ApiError({
        timestamp: "2026-07-25T12:00:00Z",
        status: 409,
        error: "Conflict",
        message: "The incident cannot be escalated",
        path: "/api/incidents/42/escalations",
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

    await user.click(
      screen.getByRole("button", { name: "Escalate incident" }),
    )
    expect(
      await screen.findByText(
        "Enter a reason before escalating the incident.",
      ),
    ).toBeInTheDocument()
    expect(mockEscalateIncident).not.toHaveBeenCalled()

    const reasonField = screen.getByLabelText("Escalation reason")
    await user.type(reasonField, reason)
    await user.click(
      screen.getByRole("button", { name: "Escalate incident" }),
    )

    expect(
      await screen.findByText("The incident cannot be escalated"),
    ).toBeInTheDocument()
    expect(reasonField).toHaveValue(reason)
  })

  it("hides the escalation form for resolved incidents", () => {
    render(
      <IncidentDetailPanel
        incidentId={incident.id}
        initialIncident={{
          ...incident,
          status: "RESOLVED",
          acknowledgedAt: "2026-07-25T08:16:30Z",
          resolvedAt: "2026-07-25T08:20:30Z",
          allowedTransitions: ["CLOSED", "INVESTIGATING"],
        }}
        onUpdated={vi.fn()}
      />,
    )

    expect(
      screen.getByText(
        "Resolved and closed incidents cannot be escalated.",
      ),
    ).toBeInTheDocument()
    expect(
      screen.queryByLabelText("Escalation reason"),
    ).not.toBeInTheDocument()
  })
})
