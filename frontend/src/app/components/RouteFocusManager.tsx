import { useEffect } from "react"
import { Outlet, useLocation } from "react-router-dom"

export function RouteFocusManager() {
  const { pathname } = useLocation()

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      document.querySelector<HTMLElement>("main h1")?.focus()
    })

    return () => window.cancelAnimationFrame(frame)
  }, [pathname])

  return <Outlet />
}
