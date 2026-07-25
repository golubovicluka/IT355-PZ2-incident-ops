package com.golubovicluka.incident_ops.identity.application;

public class RegistrationUnavailableException extends RuntimeException {

	public RegistrationUnavailableException() {
		super("Registration is temporarily unavailable");
	}
}
