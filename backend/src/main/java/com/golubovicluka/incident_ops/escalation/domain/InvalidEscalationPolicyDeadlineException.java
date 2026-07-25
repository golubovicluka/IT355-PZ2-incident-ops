package com.golubovicluka.incident_ops.escalation.domain;

public class InvalidEscalationPolicyDeadlineException extends RuntimeException {

	private final String field;

	public InvalidEscalationPolicyDeadlineException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}
}
