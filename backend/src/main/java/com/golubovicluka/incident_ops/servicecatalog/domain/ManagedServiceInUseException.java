package com.golubovicluka.incident_ops.servicecatalog.domain;

public class ManagedServiceInUseException extends RuntimeException {

	public ManagedServiceInUseException() {
		super("Managed service cannot be deleted while other records reference it");
	}

	public ManagedServiceInUseException(Throwable cause) {
		super("Managed service cannot be deleted while other records reference it", cause);
	}
}
