package com.golubovicluka.incident_ops.incident.domain;

public class IncidentNotFoundException extends RuntimeException {

	public IncidentNotFoundException() {
		super("Incident does not exist");
	}
}
