package com.golubovicluka.incident_ops.identity.domain;

public class DuplicateTeamNameException extends RuntimeException {

	public DuplicateTeamNameException() {
		super("A team with this name already exists");
	}

	public DuplicateTeamNameException(Throwable cause) {
		super("A team with this name already exists", cause);
	}
}
