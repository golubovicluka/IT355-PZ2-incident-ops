import { AuthenticationCard } from "@/features/authentication/components/AuthenticationCard"
import { LoginForm } from "@/features/authentication/components/LoginForm"

export function SignInPage() {
  return (
    <main className="flex min-h-svh items-center justify-center bg-muted/40 p-4 sm:p-6">
      <AuthenticationCard
        alternateAction={{ label: "Register", to: "/register" }}
        description="Use your IncidentOps username and password to continue."
        title="Welcome back"
      >
        <LoginForm />
      </AuthenticationCard>
    </main>
  )
}
