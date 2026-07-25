package com.golubovicluka.incident_ops.incident.application;

public class IncidentActorNotFoundException extends RuntimeException {

	public IncidentActorNotFoundException() {
		super("Authenticated incident actor does not exist");
	}
}
