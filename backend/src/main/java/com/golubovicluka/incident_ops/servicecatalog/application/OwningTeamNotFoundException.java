package com.golubovicluka.incident_ops.servicecatalog.application;

public class OwningTeamNotFoundException extends RuntimeException {

	public OwningTeamNotFoundException() {
		super("Owning team does not exist");
	}
}
