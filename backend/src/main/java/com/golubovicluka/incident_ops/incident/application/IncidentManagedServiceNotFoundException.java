package com.golubovicluka.incident_ops.incident.application;

public class IncidentManagedServiceNotFoundException extends RuntimeException {

	public IncidentManagedServiceNotFoundException() {
		super("Managed service does not exist");
	}
}
