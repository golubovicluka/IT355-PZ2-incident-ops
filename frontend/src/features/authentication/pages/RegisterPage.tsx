import { AuthenticationCard } from "@/features/authentication/components/AuthenticationCard"
import { RegisterForm } from "@/features/authentication/components/RegisterForm"

export function RegisterPage() {
  return (
    <main className="flex min-h-svh items-center justify-center bg-muted/40 p-4 sm:p-6">
      <AuthenticationCard
        alternateAction={{ label: "Sign in", to: "/sign-in" }}
        description="Create your responder profile. Team and role access are managed separately."
        title="Create your account"
      >
        <RegisterForm />
      </AuthenticationCard>
    </main>
  )
}
