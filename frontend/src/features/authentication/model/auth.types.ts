export interface LoginValues {
  username: string
  password: string
}

export interface RegisterValues {
  displayName: string
  username: string
  password: string
  confirmPassword: string
}

export interface RegistrationRequest {
  displayName: string
  username: string
  password: string
}

export interface RegisteredUserAccount {
  id: number
  username: string
  displayName: string
  roles: string[]
  team: {
    id: number
    name: string
  }
}

export type FieldErrors<T> = Partial<Record<keyof T, string>>
