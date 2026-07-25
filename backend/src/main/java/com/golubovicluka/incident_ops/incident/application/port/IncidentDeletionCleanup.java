package com.golubovicluka.incident_ops.incident.application.port;

/**
 * Application port for feature-owned records that must be removed before an
 * incident aggregate can be deleted.
 */
@FunctionalInterface
public interface IncidentDeletionCleanup {

	void deleteForIncident(long incidentId);
}
