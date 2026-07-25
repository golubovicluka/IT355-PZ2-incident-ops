package com.golubovicluka.incident_ops.identity.domain;

public class DuplicateUsernameException extends RuntimeException {

	public DuplicateUsernameException() {
		super("Username is already registered");
	}

	public DuplicateUsernameException(Throwable cause) {
		super("Username is already registered", cause);
	}
}
