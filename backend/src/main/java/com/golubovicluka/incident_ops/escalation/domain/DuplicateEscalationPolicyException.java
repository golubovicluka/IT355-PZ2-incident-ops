package com.golubovicluka.incident_ops.escalation.domain;

public class DuplicateEscalationPolicyException extends RuntimeException {

	public DuplicateEscalationPolicyException() {
		super("An escalation policy already exists for this service and priority");
	}

	public DuplicateEscalationPolicyException(Throwable cause) {
		super("An escalation policy already exists for this service and priority", cause);
	}
}
