package com.golubovicluka.incident_ops.escalation.domain;

import java.util.Objects;

public record PolicyManagedService(Long id, String name) {

	public PolicyManagedService {
		if (id == null || id <= 0) {
			throw new IllegalArgumentException("managed service id must be positive");
		}
		Objects.requireNonNull(name, "managed service name must not be null");
		name = name.strip();
		if (name.isEmpty()) {
			throw new IllegalArgumentException("managed service name must not be blank");
		}
	}
}
