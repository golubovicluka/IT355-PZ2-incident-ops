package com.golubovicluka.incident_ops.servicecatalog.domain;

public class ManagedServiceNotFoundException extends RuntimeException {

	public ManagedServiceNotFoundException() {
		super("Managed service was not found");
	}
}
