package com.golubovicluka.incident_ops.incident.application;

public class IncidentAssigneeNotFoundException extends RuntimeException {

	public IncidentAssigneeNotFoundException() {
		super("Assignee does not exist");
	}
}
