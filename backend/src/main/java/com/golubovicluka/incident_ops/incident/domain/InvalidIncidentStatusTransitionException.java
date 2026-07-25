package com.golubovicluka.incident_ops.incident.domain;

public class InvalidIncidentStatusTransitionException extends RuntimeException {

	public InvalidIncidentStatusTransitionException(
			IncidentStatus currentStatus,
			IncidentStatus requestedStatus) {
		super("Incident status cannot transition from %s to %s".formatted(
				currentStatus,
				requestedStatus));
	}
}
