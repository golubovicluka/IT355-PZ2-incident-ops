import { useContext } from "react"

import { AuthenticationContext } from "@/features/authentication/context/authentication-context"

export function useAuthentication() {
  const context = useContext(AuthenticationContext)

  if (!context) {
    throw new Error(
      "useAuthentication must be used inside AuthenticationProvider.",
    )
  }

  return context
}
