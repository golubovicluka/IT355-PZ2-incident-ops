import type {
  Criticality,
  ManagedServiceCatalogItem,
} from "@/shared/catalogs/catalog.types"

export type ManagedService = ManagedServiceCatalogItem

export interface ManagedServiceRequest {
  name: string
  description: string
  criticality: Criticality
  owningTeamId: number
}
