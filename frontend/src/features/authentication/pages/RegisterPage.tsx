import { AuthenticationCard } from "@/features/authentication/components/AuthenticationCard"
import { RegisterForm } from "@/features/authentication/components/RegisterForm"

export function RegisterPage() {
  return (
    <main className="flex min-h-svh items-center justify-center bg-muted/40 p-4 sm:p-6">
      <AuthenticationCard
        alternateAction={{ label: "Sign in", to: "/sign-in" }}
        description="Create an IncidentOps responder account."
        footerDescription="New accounts receive responder access and join the Incident Response team."
        title="Create account"
      >
        <RegisterForm />
      </AuthenticationCard>
    </main>
  )
}
