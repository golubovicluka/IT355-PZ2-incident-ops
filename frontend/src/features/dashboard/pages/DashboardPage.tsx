import { useOutletContext } from "react-router-dom"

interface AppOutletContext {
  displayName: string
}

export function DashboardPage() {
  const { displayName } = useOutletContext<AppOutletContext>()

  return (
    <section className="rounded-xl border bg-card p-8 shadow-sm">
      <p className="text-sm font-medium text-muted-foreground">IncidentOps</p>
      <h1 className="mt-2 text-3xl font-semibold tracking-tight">
        Operations overview
      </h1>
      <p className="mt-3 max-w-2xl text-muted-foreground">
        Welcome, {displayName}. Track incidents, coordinate responders,
        and keep resolution work visible.
      </p>
    </section>
  )
}
