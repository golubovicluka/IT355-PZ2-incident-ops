export interface CatalogTeam {
  id: number
  name: string
}

export type Criticality = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"

export interface ManagedServiceCatalogItem {
  id: number
  name: string
  description: string
  criticality: Criticality
  owningTeam: CatalogTeam
}

export interface AssignableUserCatalogItem {
  id: number
  username: string
  displayName: string
  team: CatalogTeam
}
