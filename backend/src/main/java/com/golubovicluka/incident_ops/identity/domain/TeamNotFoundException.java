package com.golubovicluka.incident_ops.identity.domain;

public class TeamNotFoundException extends RuntimeException {

	public TeamNotFoundException() {
		super("Team was not found");
	}
}
