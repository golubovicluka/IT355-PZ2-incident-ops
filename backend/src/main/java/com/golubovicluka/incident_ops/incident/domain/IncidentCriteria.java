package com.golubovicluka.incident_ops.incident.domain;

public record IncidentCriteria(
		IncidentStatus status,
		IncidentPriority priority,
		Long managedServiceId) {

	public IncidentCriteria {
		if (managedServiceId != null && managedServiceId <= 0) {
			throw new IllegalArgumentException(
					"managedServiceId must be positive");
		}
	}
}
