import type { IncidentPriority } from "@/features/administration/model/policy.types"

export interface PolicyFormValues {
  managedServiceId: number | null
  priority: IncidentPriority | null
  acknowledgementMinutes: string
  resolutionMinutes: string
}

export interface PolicyFormErrors {
  managedServiceId?: string
  priority?: string
  acknowledgementMinutes?: string
  resolutionMinutes?: string
}

function parsePositiveWholeMinutes(value: string) {
  const normalized = value.trim()

  if (!normalized) {
    return undefined
  }

  const minutes = Number(normalized)
  return Number.isSafeInteger(minutes) && minutes > 0
    ? minutes
    : undefined
}

export function validatePolicyForm(values: PolicyFormValues) {
  const errors: PolicyFormErrors = {}
  const acknowledgementMinutes = parsePositiveWholeMinutes(
    values.acknowledgementMinutes,
  )
  const resolutionMinutes = parsePositiveWholeMinutes(
    values.resolutionMinutes,
  )

  if (!values.managedServiceId) {
    errors.managedServiceId = "Select a managed service."
  }

  if (!values.priority) {
    errors.priority = "Select an incident priority."
  }

  if (!acknowledgementMinutes) {
    errors.acknowledgementMinutes =
      "Enter a positive whole-minute acknowledgement deadline."
  }

  if (!resolutionMinutes) {
    errors.resolutionMinutes =
      "Enter a positive whole-minute resolution deadline."
  }

  if (
    acknowledgementMinutes &&
    resolutionMinutes &&
    acknowledgementMinutes > resolutionMinutes
  ) {
    errors.acknowledgementMinutes =
      "Acknowledgement deadline must not exceed the resolution deadline."
  }

  return errors
}

export function toPositiveWholeMinutes(value: string) {
  return parsePositiveWholeMinutes(value)
}
