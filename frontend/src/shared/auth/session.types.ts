export type SessionRole = "RESPONDER" | "ADMIN"

export interface AuthSession {
  token: string
  expiresAt: string
  username: string
  displayName: string
  roles: SessionRole[]
}
