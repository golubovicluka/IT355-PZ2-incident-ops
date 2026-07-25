package com.golubovicluka.incident_ops.incident.domain;

public class DuplicateIncidentReferenceCodeException extends RuntimeException {

	public DuplicateIncidentReferenceCodeException(Throwable cause) {
		super("An incident with this reference code already exists", cause);
	}
}
