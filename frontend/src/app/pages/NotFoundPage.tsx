import { Link } from "react-router-dom"

export function NotFoundPage() {
  return (
    <section className="rounded-xl border bg-card p-8 text-center shadow-sm">
      <p className="text-sm font-medium text-muted-foreground">Error 404</p>
      <h1
        className="mt-2 text-3xl font-semibold tracking-tight"
        tabIndex={-1}
      >
        Page not found
      </h1>
      <p className="mt-3 text-muted-foreground">
        The page you requested does not exist or may have moved.
      </p>
      <Link
        className="mt-6 inline-flex text-sm font-medium text-primary underline-offset-4 hover:underline"
        to="/"
      >
        Return to overview
      </Link>
    </section>
  )
}
