import { ApiError } from "@/shared/api/api-error"
import {
  API_FORBIDDEN_EVENT,
  API_UNAUTHORIZED_EVENT,
} from "@/shared/api/api-events"
import type {
  ApiErrorResponse,
  ApiFieldErrors,
} from "@/shared/api/api.types"
import { getAccessToken } from "@/shared/auth/session-storage"

interface ApiRequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown
  token?: string
}

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()
const apiBaseUrl = (
  configuredBaseUrl || (import.meta.env.DEV ? "http://localhost:8081" : "")
).replace(/\/+$/, "")

function requestUrl(path: string) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`
  return `${apiBaseUrl}${normalizedPath}`
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null
}

function parseFieldErrors(value: unknown): ApiFieldErrors {
  if (!isRecord(value)) {
    return {}
  }

  return Object.fromEntries(
    Object.entries(value).filter(
      (entry): entry is [string, string] => typeof entry[1] === "string",
    ),
  )
}

function toApiError(status: number, path: string, payload: unknown) {
  const body = isRecord(payload) ? payload : {}
  const response: ApiErrorResponse = {
    timestamp:
      typeof body.timestamp === "string"
        ? body.timestamp
        : new Date().toISOString(),
    status: typeof body.status === "number" ? body.status : status,
    error: typeof body.error === "string" ? body.error : "Request failed",
    message:
      typeof body.message === "string"
        ? body.message
        : "The request could not be completed.",
    path: typeof body.path === "string" ? body.path : path,
    fieldErrors: parseFieldErrors(body.fieldErrors),
  }

  return new ApiError(response)
}

async function parseResponse(response: Response) {
  if (response.status === 204) {
    return undefined
  }

  const contentType = response.headers.get("content-type") ?? ""
  if (contentType.includes("application/json")) {
    return response.json()
  }

  const text = await response.text()
  return text || undefined
}

async function request<T>(path: string, options: ApiRequestOptions = {}) {
  const { body, token, ...requestOptions } = options
  const headers = new Headers(requestOptions.headers)
  const accessToken = token ?? getAccessToken()

  if (body !== undefined) {
    headers.set("Content-Type", "application/json")
  }

  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`)
  }

  const response = await fetch(requestUrl(path), {
    ...requestOptions,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const payload = await parseResponse(response)

  if (!response.ok) {
    const error = toApiError(response.status, path, payload)

    if (error.status === 401 && path.replace(/^\/+/, "") !== "login") {
      window.dispatchEvent(
        new CustomEvent(API_UNAUTHORIZED_EVENT, { detail: error }),
      )
    } else if (error.status === 403) {
      window.dispatchEvent(
        new CustomEvent(API_FORBIDDEN_EVENT, { detail: error }),
      )
    }

    throw error
  }

  return payload as T
}

export const apiClient = {
  get<T>(path: string, options?: ApiRequestOptions) {
    return request<T>(path, { ...options, method: "GET" })
  },
  post<T>(path: string, body?: unknown, options?: ApiRequestOptions) {
    return request<T>(path, { ...options, method: "POST", body })
  },
  put<T>(path: string, body?: unknown, options?: ApiRequestOptions) {
    return request<T>(path, { ...options, method: "PUT", body })
  },
  delete<T>(path: string, options?: ApiRequestOptions) {
    return request<T>(path, { ...options, method: "DELETE" })
  },
}
