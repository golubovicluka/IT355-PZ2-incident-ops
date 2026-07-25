import { ShieldXIcon } from "lucide-react"
import { Link, Outlet } from "react-router-dom"

import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert"
import { buttonVariants } from "@/components/ui/button"
import { useAuthentication } from "@/features/authentication/context/useAuthentication"

export function AdminRoute() {
  const { hasRole } = useAuthentication()

  if (!hasRole("ADMIN")) {
    return (
      <section className="mx-auto max-w-2xl">
        <Alert variant="destructive">
          <ShieldXIcon />
          <AlertTitle>
            <h1 tabIndex={-1}>Administrator access required</h1>
          </AlertTitle>
          <AlertDescription>
            <p>
              Your session is still active, but you do not have permission to
              view this page.
            </p>
            <Link
              className={buttonVariants({
                className: "mt-4",
                size: "sm",
                variant: "outline",
              })}
              to="/dashboard"
            >
              Return to dashboard
            </Link>
          </AlertDescription>
        </Alert>
      </section>
    )
  }

  return <Outlet />
}
