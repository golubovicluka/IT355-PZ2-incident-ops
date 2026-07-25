package com.golubovicluka.incident_ops.escalation.application;

public class PolicyManagedServiceNotFoundException extends RuntimeException {

	public PolicyManagedServiceNotFoundException() {
		super("Managed service does not exist");
	}
}
