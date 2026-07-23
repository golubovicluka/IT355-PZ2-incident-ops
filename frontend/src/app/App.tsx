import { NavLink, Outlet } from "react-router-dom"

function App() {
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
          <nav aria-label="Primary navigation">
            <NavLink
              className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
              to="/"
            >
              Overview
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl px-6 py-10">
        <Outlet />
      </main>
    </div>
  )
}

export default App
