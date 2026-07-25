import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation,
} from "react-router-dom"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { registerAccount } from "@/features/authentication/api/registration-api"
import { RegisterForm } from "@/features/authentication/components/RegisterForm"
import { ApiError } from "@/shared/api/api-error"

vi.mock("@/features/authentication/api/registration-api", () => ({
  registerAccount: vi.fn(),
}))

const mockRegisterAccount = vi.mocked(registerAccount)

function SignInDestination() {
  const location = useLocation()
  const message =
    typeof location.state === "object" &&
    location.state !== null &&
    "message" in location.state &&
    typeof location.state.message === "string"
      ? location.state.message
      : ""

  return <p>{message}</p>
}

function renderForm() {
  render(
    <MemoryRouter initialEntries={["/register"]}>
      <Routes>
        <Route path="/register" element={<RegisterForm />} />
        <Route path="/sign-in" element={<SignInDestination />} />
      </Routes>
    </MemoryRouter>,
  )
}

async function fillValidForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Display name"), " New Engineer ")
  await user.type(screen.getByLabelText("Username"), " New.Responder ")
  await user.type(screen.getByLabelText("Password"), "strong-password")
  await user.type(
    screen.getByLabelText("Confirm password"),
    "strong-password",
  )
}

describe("RegisterForm", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("labels the submit action Register and validates all required fields", async () => {
    const user = userEvent.setup()
    renderForm()

    await user.click(screen.getByRole("button", { name: "Register" }))

    expect(screen.getByText("Enter the name your team will recognize."))
      .toBeInTheDocument()
    expect(screen.getByText("Enter your username.")).toBeInTheDocument()
    expect(screen.getByText("Enter your password.")).toBeInTheDocument()
    expect(screen.getByText("Confirm your password.")).toBeInTheDocument()
    expect(mockRegisterAccount).not.toHaveBeenCalled()
  })

  it("creates the account and redirects to sign-in with confirmation", async () => {
    const user = userEvent.setup()
    mockRegisterAccount.mockResolvedValue({
      id: 42,
      username: "new.responder",
      displayName: "New Engineer",
      roles: ["RESPONDER"],
      team: {
        id: 7,
        name: "Incident Response",
      },
    })
    renderForm()

    await fillValidForm(user)
    await user.click(screen.getByRole("button", { name: "Register" }))

    expect(mockRegisterAccount).toHaveBeenCalledWith({
      displayName: "New Engineer",
      username: "New.Responder",
      password: "strong-password",
    })
    expect(
      await screen.findByText(
        "Account created for new.responder. Sign in to continue.",
      ),
    ).toBeInTheDocument()
  })

  it("maps server field errors and preserves entered values", async () => {
    const user = userEvent.setup()
    mockRegisterAccount.mockRejectedValue(
      new ApiError({
        timestamp: "2026-07-26T00:00:00Z",
        status: 409,
        error: "Conflict",
        message: "Username is already registered",
        path: "/register",
        fieldErrors: {
          username: "Username is already registered",
        },
      }),
    )
    renderForm()

    await fillValidForm(user)
    await user.click(screen.getByRole("button", { name: "Register" }))

    expect(
      await screen.findByText("Username is already registered"),
    ).toBeInTheDocument()
    expect(screen.getByLabelText("Display name")).toHaveValue(
      " New Engineer ",
    )
    expect(screen.getByLabelText("Username")).toHaveValue(" New.Responder ")
    expect(screen.getByLabelText("Password")).toHaveValue("strong-password")
    expect(screen.getByLabelText("Confirm password")).toHaveValue(
      "strong-password",
    )
  })

  it("uses a neutral message for unexpected registration failures", async () => {
    const user = userEvent.setup()
    mockRegisterAccount.mockRejectedValue(new TypeError("Failed to fetch"))
    renderForm()

    await fillValidForm(user)
    await user.click(screen.getByRole("button", { name: "Register" }))

    expect(
      await screen.findByText("Registration is unavailable. Try again."),
    ).toBeInTheDocument()
    expect(screen.queryByText("Failed to fetch")).not.toBeInTheDocument()
    expect(screen.getByLabelText("Username")).toHaveValue(" New.Responder ")
  })

  it("locks every field and the button while registration is pending", async () => {
    const user = userEvent.setup()
    mockRegisterAccount.mockImplementation(
      () =>
        new Promise<never>(() => {
          // Intentionally unresolved so the pending state remains observable.
        }),
    )
    renderForm()

    await fillValidForm(user)
    await user.click(screen.getByRole("button", { name: "Register" }))

    expect(
      screen.getByRole("button", { name: "Registering…" }),
    ).toBeDisabled()
    expect(screen.getByLabelText("Display name")).toBeDisabled()
    expect(screen.getByLabelText("Username")).toBeDisabled()
    expect(screen.getByLabelText("Password")).toBeDisabled()
    expect(screen.getByLabelText("Confirm password")).toBeDisabled()
    expect(mockRegisterAccount).toHaveBeenCalledTimes(1)
  })
})
