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

export type FieldErrors<T> = Partial<Record<keyof T, string>>
