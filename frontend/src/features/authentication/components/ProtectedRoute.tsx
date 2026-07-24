import { Navigate, Outlet, useLocation } from "react-router-dom"

import { useAuthentication } from "@/features/authentication/context/useAuthentication"

export function ProtectedRoute() {
  const location = useLocation()
  const { session } = useAuthentication()

  if (!session) {
    return (
      <Navigate
        replace
        state={{ from: location.pathname }}
        to="/sign-in"
      />
    )
  }

  return <Outlet />
}
