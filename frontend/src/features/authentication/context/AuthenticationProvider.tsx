import {
  useEffect,
  useState,
  type ReactNode,
} from "react"
import { useNavigate } from "react-router-dom"

import type { LoginValues } from "@/features/authentication/model/auth.types"
import {
  AuthenticationContext,
  type AuthenticationContextValue,
} from "@/features/authentication/context/authentication-context"
import { apiClient } from "@/shared/api/api-client"
import { ApiError } from "@/shared/api/api-error"
import {
  API_FORBIDDEN_EVENT,
  API_UNAUTHORIZED_EVENT,
} from "@/shared/api/api-events"
import {
  clearSession,
  loadSession,
  parseSession,
  saveSession,
} from "@/shared/auth/session-storage"
import type { AuthSession } from "@/shared/auth/session.types"

export function AuthenticationProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [session, setSession] = useState<AuthSession | null>(loadSession)
  const [authenticationMessage, setAuthenticationMessage] = useState<
    string | null
  >(null)
  const [permissionMessage, setPermissionMessage] = useState<string | null>(null)

  useEffect(() => {
    function handleUnauthorized() {
      clearSession()
      setSession(null)
      setAuthenticationMessage(
        "Your session expired. Sign in again to continue.",
      )
      setPermissionMessage(null)
      navigate("/sign-in", {
        replace: true,
        state: {
          message: "Your session expired. Sign in again to continue.",
        },
      })
    }

    function handleForbidden(event: Event) {
      const error =
        event instanceof CustomEvent && event.detail instanceof ApiError
          ? event.detail
          : null
      setPermissionMessage(
        error?.message || "You do not have permission for that action.",
      )
    }

    window.addEventListener(API_UNAUTHORIZED_EVENT, handleUnauthorized)
    window.addEventListener(API_FORBIDDEN_EVENT, handleForbidden)

    return () => {
      window.removeEventListener(API_UNAUTHORIZED_EVENT, handleUnauthorized)
      window.removeEventListener(API_FORBIDDEN_EVENT, handleForbidden)
    }
  }, [navigate])

  async function login(credentials: LoginValues) {
    const response = await apiClient.post<unknown>("/login", credentials)
    const nextSession = parseSession(response)

    if (!nextSession) {
      throw new Error("The authentication service returned an invalid session.")
    }

    saveSession(nextSession)
    setSession(nextSession)
    setAuthenticationMessage(null)
    setPermissionMessage(null)
    return nextSession
  }

  function logout() {
    clearSession()
    setSession(null)
    setAuthenticationMessage(null)
    setPermissionMessage(null)
    navigate("/sign-in", { replace: true })
  }

  const value: AuthenticationContextValue = {
    session,
    authenticationMessage,
    permissionMessage,
    login,
    logout,
    hasRole: (role) => session?.roles.includes(role) ?? false,
    clearPermissionMessage: () => setPermissionMessage(null),
  }

  return (
    <AuthenticationContext.Provider value={value}>
      {children}
    </AuthenticationContext.Provider>
  )
}
