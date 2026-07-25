package com.golubovicluka.incident_ops.incident.application;

public class IncidentReporterNotFoundException extends RuntimeException {

	public IncidentReporterNotFoundException() {
		super("Authenticated reporter does not exist");
	}
}
