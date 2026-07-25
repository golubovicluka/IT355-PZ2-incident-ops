package com.golubovicluka.incident_ops.escalation.domain;

public class EscalationPolicyNotFoundException extends RuntimeException {

	public EscalationPolicyNotFoundException() {
		super("Escalation policy was not found");
	}
}
