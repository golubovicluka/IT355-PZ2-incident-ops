import type {
  AuthSession,
  SessionRole,
} from "@/shared/auth/session.types"

const SESSION_STORAGE_KEY = "incident-ops.session"
const SUPPORTED_ROLES: SessionRole[] = ["RESPONDER", "ADMIN"]

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null
}

export function parseSession(value: unknown): AuthSession | null {
  if (!isRecord(value) || !Array.isArray(value.roles)) {
    return null
  }

  const roles = value.roles.filter(
    (role): role is SessionRole =>
      typeof role === "string" &&
      SUPPORTED_ROLES.includes(role as SessionRole),
  )

  if (
    typeof value.token !== "string" ||
    typeof value.expiresAt !== "string" ||
    typeof value.username !== "string" ||
    typeof value.displayName !== "string" ||
    !value.token ||
    !value.username ||
    !value.displayName ||
    roles.length === 0 ||
    roles.length !== value.roles.length ||
    Number.isNaN(Date.parse(value.expiresAt)) ||
    Date.parse(value.expiresAt) <= Date.now()
  ) {
    return null
  }

  return {
    token: value.token,
    expiresAt: value.expiresAt,
    username: value.username,
    displayName: value.displayName,
    roles,
  }
}

export function loadSession() {
  const storedSession = sessionStorage.getItem(SESSION_STORAGE_KEY)

  if (!storedSession) {
    return null
  }

  try {
    const session = parseSession(JSON.parse(storedSession))

    if (!session) {
      clearSession()
      return null
    }

    return session
  } catch {
    clearSession()
    return null
  }
}

export function saveSession(session: AuthSession) {
  sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session))
}

export function clearSession() {
  sessionStorage.removeItem(SESSION_STORAGE_KEY)
}

export function getAccessToken() {
  return loadSession()?.token
}
