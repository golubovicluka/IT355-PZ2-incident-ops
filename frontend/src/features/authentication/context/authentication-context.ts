import { createContext } from "react"

import type { LoginValues } from "@/features/authentication/model/auth.types"
import type {
  AuthSession,
  SessionRole,
} from "@/shared/auth/session.types"

export interface AuthenticationContextValue {
  session: AuthSession | null
  authenticationMessage: string | null
  permissionMessage: string | null
  login: (credentials: LoginValues) => Promise<AuthSession>
  logout: () => void
  hasRole: (role: SessionRole) => boolean
  clearPermissionMessage: () => void
}

export const AuthenticationContext =
  createContext<AuthenticationContextValue | null>(null)
