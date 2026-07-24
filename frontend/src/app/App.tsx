import { LogOutIcon, ShieldIcon, XIcon } from "lucide-react"
import { NavLink, Outlet } from "react-router-dom"

import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { useAuthentication } from "@/features/authentication"

function App() {
  const {
    session,
    permissionMessage,
    logout,
    hasRole,
    clearPermissionMessage,
  } = useAuthentication()

  return (
    <div className="min-h-svh bg-muted/40">
      <header className="border-b bg-background">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
          <NavLink
            className="text-lg font-semibold tracking-tight"
            to="/"
          >
            IncidentOps
          </NavLink>
          <nav
            aria-label="Primary navigation"
            className="flex items-center gap-4"
          >
            <NavLink
              className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
              to="/dashboard"
            >
              Overview
            </NavLink>
            {hasRole("ADMIN") ? (
              <NavLink
                className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
                to="/admin"
              >
                Admin
              </NavLink>
            ) : null}
            <span className="hidden text-sm text-muted-foreground sm:inline">
              {session?.displayName}
            </span>
            <Button onClick={logout} size="sm" variant="outline">
              <LogOutIcon data-icon="inline-start" />
              Sign out
            </Button>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl px-6 py-10">
        {permissionMessage ? (
          <Alert className="mb-6" variant="destructive">
            <ShieldIcon />
            <AlertTitle>Permission denied</AlertTitle>
            <AlertDescription>{permissionMessage}</AlertDescription>
            <Button
              aria-label="Dismiss permission message"
              className="ml-auto"
              onClick={clearPermissionMessage}
              size="icon-sm"
              variant="ghost"
            >
              <XIcon />
            </Button>
          </Alert>
        ) : null}
        <Outlet context={{ displayName: session?.displayName ?? "" }} />
      </main>
    </div>
  )
}

export default App
