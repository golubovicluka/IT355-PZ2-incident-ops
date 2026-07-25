package com.golubovicluka.incident_ops.incident.domain;

public class IncidentEscalationNotAllowedException extends RuntimeException {

	public IncidentEscalationNotAllowedException(IncidentStatus status) {
		super("Incident cannot be escalated while its status is " + status);
	}
}
