package com.golubovicluka.incident_ops.identity.domain;

public class TeamInUseException extends RuntimeException {

	public TeamInUseException() {
		super("Team cannot be deleted while users or services reference it");
	}

	public TeamInUseException(Throwable cause) {
		super("Team cannot be deleted while users or services reference it", cause);
	}
}
