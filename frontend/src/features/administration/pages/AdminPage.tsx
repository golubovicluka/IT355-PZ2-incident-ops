import { ShieldCheckIcon } from "lucide-react"

import { Badge } from "@/components/ui/badge"

export function AdminPage() {
  return (
    <section className="rounded-xl border bg-card p-8 shadow-sm">
      <Badge variant="secondary">
        <ShieldCheckIcon data-icon="inline-start" />
        Administrator
      </Badge>
      <h1 className="mt-4 text-3xl font-semibold tracking-tight">
        Administration
      </h1>
      <p className="mt-3 max-w-2xl text-muted-foreground">
        Administrative controls are available only to authenticated users with
        the ADMIN role.
      </p>
    </section>
  )
}
