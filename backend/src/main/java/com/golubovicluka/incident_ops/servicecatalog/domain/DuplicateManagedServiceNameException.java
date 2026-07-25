package com.golubovicluka.incident_ops.servicecatalog.domain;

public class DuplicateManagedServiceNameException extends RuntimeException {

	public DuplicateManagedServiceNameException() {
		super("A managed service with this name already exists");
	}

	public DuplicateManagedServiceNameException(Throwable cause) {
		super("A managed service with this name already exists", cause);
	}
}
