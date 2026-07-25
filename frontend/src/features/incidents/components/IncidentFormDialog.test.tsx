import { createRef } from "react"

import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { IncidentFormDialog } from "@/features/incidents/components/IncidentFormDialog"
import { ApiError } from "@/shared/api/api-error"
import {
  listAssignableUserCatalog,
  listServiceCatalog,
} from "@/shared/catalogs/catalog-api"

vi.mock("@/shared/catalogs/catalog-api", () => ({
  listAssignableUserCatalog: vi.fn(),
  listServiceCatalog: vi.fn(),
}))

const mockListServices = vi.mocked(listServiceCatalog)
const mockListUsers = vi.mocked(listAssignableUserCatalog)

async function fillValidCreateForm(user: ReturnType<typeof userEvent.setup>) {
  const title = await screen.findByLabelText("Title")
  const description = screen.getByLabelText("Description")

  await user.type(title, "Checkout failures")
  await user.type(description, "Card payments are timing out.")
  await user.click(screen.getByRole("combobox", { name: "Priority" }))
  await user.click(screen.getByRole("option", { name: "SEV1" }))
  await user.click(
    screen.getByRole("combobox", { name: "Managed service" }),
  )
  await user.click(screen.getByRole("option", { name: "Payments API" }))
  await user.click(screen.getByRole("combobox", { name: "Assignee" }))
  await user.click(screen.getByRole("option", { name: /Ana Anić/ }))

  return { description, title }
}

describe("IncidentFormDialog", () => {
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

  it("maps server field errors and preserves every create value", async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockRejectedValue(
      new ApiError({
        timestamp: "2026-07-25T12:00:00Z",
        status: 400,
        error: "Bad Request",
        message: "Request validation failed",
        path: "/api/incidents",
        fieldErrors: {
          title: "An incident with this title is already open",
        },
      }),
    )

    render(
      <IncidentFormDialog
        finalFocusRef={createRef<HTMLElement>()}
        mode="create"
        onOpenChange={vi.fn()}
        onSubmit={onSubmit}
        onSuccess={vi.fn()}
        open
      />,
    )

    const { description, title } = await fillValidCreateForm(user)
    await user.click(screen.getByRole("button", { name: "Report incident" }))

    expect(onSubmit).toHaveBeenCalledWith({
      title: "Checkout failures",
      description: "Card payments are timing out.",
      priority: "SEV1",
      managedServiceId: 7,
      assigneeId: 12,
    })
    expect(
      await screen.findByText("An incident with this title is already open"),
    ).toBeInTheDocument()
    expect(title).toHaveValue("Checkout failures")
    expect(description).toHaveValue("Card payments are timing out.")
    expect(
      screen.getByRole("heading", { name: "Report an incident" }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole("combobox", { name: "Priority" }),
    ).toHaveTextContent("SEV1")
    expect(
      screen.getByRole("combobox", { name: "Managed service" }),
    ).toHaveTextContent("Payments API")
    expect(
      screen.getByRole("combobox", { name: "Assignee" }),
    ).toHaveTextContent("Ana Anić")
  })

  it("locks the form while a create request is pending", async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn(
      () =>
        new Promise<never>(() => {
          // Intentionally unresolved so the pending state remains observable.
        }),
    )

    render(
      <IncidentFormDialog
        finalFocusRef={createRef<HTMLElement>()}
        mode="create"
        onOpenChange={vi.fn()}
        onSubmit={onSubmit}
        onSuccess={vi.fn()}
        open
      />,
    )

    await fillValidCreateForm(user)
    await user.click(screen.getByRole("button", { name: "Report incident" }))

    expect(
      screen.getByRole("button", { name: "Reporting..." }),
    ).toBeDisabled()
    expect(screen.getByLabelText("Title")).toBeDisabled()
    expect(onSubmit).toHaveBeenCalledTimes(1)
  })

  it("explains empty service and assignee catalogs", async () => {
    mockListServices.mockResolvedValue([])
    mockListUsers.mockResolvedValue([])

    render(
      <IncidentFormDialog
        finalFocusRef={createRef<HTMLElement>()}
        mode="create"
        onOpenChange={vi.fn()}
        onSubmit={vi.fn()}
        onSuccess={vi.fn()}
        open
      />,
    )

    expect(
      await screen.findByText("No managed services available"),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        "No assignable users are available. This incident can remain unassigned.",
      ),
    ).toBeInTheDocument()
    expect(
      screen.getByRole("button", { name: "Report incident" }),
    ).toBeDisabled()
  })
})
