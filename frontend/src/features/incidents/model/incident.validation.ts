import type { IncidentPriority } from "@/features/incidents/model/incident.types"

export const INCIDENT_TITLE_MAX_LENGTH = 200
export const INCIDENT_DESCRIPTION_MAX_LENGTH = 4000

export interface IncidentFormValues {
  title: string
  description: string
  priority: IncidentPriority | null
  managedServiceId: number | null
  assigneeId: number | null
}

export type IncidentFormErrors = Partial<
  Record<keyof IncidentFormValues, string>
>

export function normalizeIncidentForm(
  values: IncidentFormValues,
): IncidentFormValues {
  return {
    ...values,
    title: values.title.trim(),
    description: values.description.trim(),
  }
}

export function validateIncidentForm(
  values: IncidentFormValues,
): IncidentFormErrors {
  const errors: IncidentFormErrors = {}

  if (!values.title.trim()) {
    errors.title = "Incident title is required"
  } else if (values.title.length > INCIDENT_TITLE_MAX_LENGTH) {
    errors.title = "Incident title must not exceed 200 characters"
  }

  if (!values.description.trim()) {
    errors.description = "Incident description is required"
  } else if (values.description.length > INCIDENT_DESCRIPTION_MAX_LENGTH) {
    errors.description =
      "Incident description must not exceed 4000 characters"
  }

  if (!values.priority) {
    errors.priority = "Incident priority is required"
  }

  if (!values.managedServiceId || values.managedServiceId <= 0) {
    errors.managedServiceId = "Managed service must be selected"
  }

  if (values.assigneeId !== null && values.assigneeId <= 0) {
    errors.assigneeId = "Assignee must be a valid user"
  }

  return errors
}
