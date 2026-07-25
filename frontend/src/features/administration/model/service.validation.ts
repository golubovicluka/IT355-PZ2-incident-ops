import type { Criticality } from "@/shared/catalogs/catalog.types"

export const SERVICE_NAME_MAX_LENGTH = 100
export const SERVICE_DESCRIPTION_MAX_LENGTH = 500

export interface ServiceFormValues {
  name: string
  description: string
  criticality: Criticality | null
  owningTeamId: number | null
}

export interface ServiceFormErrors {
  name?: string
  description?: string
  criticality?: string
  owningTeamId?: string
}

export function normalizeServiceForm(
  values: ServiceFormValues,
): ServiceFormValues {
  return {
    ...values,
    name: values.name.trim(),
    description: values.description.trim(),
  }
}

export function validateServiceForm(values: ServiceFormValues) {
  const normalized = normalizeServiceForm(values)
  const errors: ServiceFormErrors = {}

  if (!normalized.name) {
    errors.name = "Enter a managed service name."
  } else if (normalized.name.length > SERVICE_NAME_MAX_LENGTH) {
    errors.name = `Managed service name must not exceed ${SERVICE_NAME_MAX_LENGTH} characters.`
  }

  if (!normalized.description) {
    errors.description = "Enter a managed service description."
  } else if (
    normalized.description.length > SERVICE_DESCRIPTION_MAX_LENGTH
  ) {
    errors.description = `Managed service description must not exceed ${SERVICE_DESCRIPTION_MAX_LENGTH} characters.`
  }

  if (!normalized.criticality) {
    errors.criticality = "Select a criticality."
  }

  if (!normalized.owningTeamId) {
    errors.owningTeamId = "Select an owning team."
  }

  return errors
}
