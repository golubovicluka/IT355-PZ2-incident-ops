export const MANAGED_SERVICES_CHANGED_EVENT =
  "incident-ops:managed-services-changed"

export function notifyManagedServicesChanged() {
  window.dispatchEvent(new Event(MANAGED_SERVICES_CHANGED_EVENT))
}
