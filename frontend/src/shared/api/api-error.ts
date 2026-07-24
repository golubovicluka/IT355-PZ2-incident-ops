import type { ApiErrorResponse } from "@/shared/api/api.types"

export class ApiError extends Error {
  readonly status: number
  readonly error: string
  readonly path: string
  readonly fieldErrors: Record<string, string>

  constructor(response: ApiErrorResponse) {
    super(response.message)
    this.name = "ApiError"
    this.status = response.status
    this.error = response.error
    this.path = response.path
    this.fieldErrors = response.fieldErrors ?? {}
  }
}
