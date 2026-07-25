package com.golubovicluka.incident_ops.escalation.domain;

public class EscalationPolicyInUseException extends RuntimeException {

	public EscalationPolicyInUseException(Throwable cause) {
		super("Escalation policy cannot be deleted while active incidents reference it", cause);
	}
}
