import { apiClient } from "@/shared/api/api-client"
import type {
  AssignableUserCatalogItem,
  ManagedServiceCatalogItem,
} from "@/shared/catalogs/catalog.types"

export function listServiceCatalog(signal?: AbortSignal) {
  return apiClient.get<ManagedServiceCatalogItem[]>(
    "/api/catalogs/services",
    { signal },
  )
}

export function listAssignableUserCatalog(signal?: AbortSignal) {
  return apiClient.get<AssignableUserCatalogItem[]>(
    "/api/catalogs/users",
    { signal },
  )
}
