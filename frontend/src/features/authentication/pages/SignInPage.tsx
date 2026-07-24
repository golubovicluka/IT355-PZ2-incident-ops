import { AuthenticationCard } from "@/features/authentication/components/AuthenticationCard"
import { LoginForm } from "@/features/authentication/components/LoginForm"
import { useAuthentication } from "@/features/authentication/context/useAuthentication"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { InfoIcon } from "lucide-react"
import { Navigate, useLocation } from "react-router-dom"

export function SignInPage() {
  const location = useLocation()
  const { authenticationMessage, session } = useAuthentication()
  const requestedPath =
    isRecord(location.state) && typeof location.state.from === "string"
      ? location.state.from
      : "/dashboard"

  if (session) {
    return <Navigate replace to={requestedPath} />
  }

  const navigationMessage =
    isRecord(location.state) && typeof location.state.message === "string"
      ? location.state.message
      : authenticationMessage

  return (
    <main className="flex min-h-svh items-center justify-center bg-muted/40 p-4 sm:p-6">
      <AuthenticationCard
        alternateAction={{ label: "Register", to: "/register" }}
        description="Use your IncidentOps username and password to continue."
        footerDescription="Your session is stored only in this browser tab and is cleared when you sign out."
        title="Welcome back"
      >
        {navigationMessage ? (
          <Alert className="mb-6">
            <InfoIcon />
            <AlertDescription>{navigationMessage}</AlertDescription>
          </Alert>
        ) : null}
        <LoginForm />
      </AuthenticationCard>
    </main>
  )
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null
}
