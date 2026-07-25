import type {
  RegisteredUserAccount,
  RegistrationRequest,
} from "@/features/authentication/model/auth.types"
import { apiClient } from "@/shared/api/api-client"

export function registerAccount(request: RegistrationRequest) {
  return apiClient.post<RegisteredUserAccount>("/register", request)
}
