export interface LoginValues {
  username: string
  password: string
}

export type FieldErrors<T> = Partial<Record<keyof T, string>>
