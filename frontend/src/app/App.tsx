import { LogOutIcon, ShieldIcon, XIcon } from "lucide-react"
import { useEffect, useRef } from "react"
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
  const permissionAlertRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (permissionMessage) {
      window.requestAnimationFrame(() => permissionAlertRef.current?.focus())
    }
  }, [permissionMessage])

  function navigationClass({ isActive }: { isActive: boolean }) {
    return [
      "rounded-md px-1 py-1 text-sm font-medium transition-colors outline-none",
      "focus-visible:ring-3 focus-visible:ring-ring/50",
      isActive
        ? "text-foreground underline decoration-2 underline-offset-4"
        : "text-muted-foreground hover:text-foreground",
    ].join(" ")
  }

  return (
    <div className="min-h-svh bg-muted/40">
      <header className="border-b bg-background">
        <div className="mx-auto flex min-h-16 max-w-6xl items-center justify-between gap-3 px-4 py-3 sm:px-6">
          <NavLink
            className="shrink-0 text-lg font-semibold tracking-tight"
            to="/"
          >
            IncidentOps
          </NavLink>
          <nav
            aria-label="Primary navigation"
            className="flex min-w-0 items-center gap-2 sm:gap-4"
          >
            <NavLink
              className={navigationClass}
              to="/dashboard"
            >
              Overview
            </NavLink>
            {hasRole("ADMIN") ? (
              <NavLink
                className={navigationClass}
                to="/admin"
              >
                Admin
              </NavLink>
            ) : null}
            <span className="hidden text-sm text-muted-foreground md:inline">
              {session?.displayName}
            </span>
            <Button
              aria-label="Sign out"
              onClick={logout}
              size="sm"
              variant="outline"
            >
              <LogOutIcon data-icon="inline-start" />
              <span className="hidden sm:inline">Sign out</span>
            </Button>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl px-4 py-6 sm:px-6 sm:py-10">
        {permissionMessage ? (
          <Alert
            className="mb-6"
            ref={permissionAlertRef}
            tabIndex={-1}
            variant="destructive"
          >
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
