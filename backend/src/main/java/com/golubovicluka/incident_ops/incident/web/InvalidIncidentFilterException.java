package com.golubovicluka.incident_ops.incident.web;

final class InvalidIncidentFilterException extends RuntimeException {

	private final String field;

	InvalidIncidentFilterException(String field, String message) {
		super(message);
		this.field = field;
	}

	String field() {
		return field;
	}
}
