package com.golubovicluka.incident_ops.servicecatalog.domain;

import java.util.Objects;

public record OwningTeam(Long id, String name) {

	public OwningTeam {
		if (id == null || id <= 0) {
			throw new IllegalArgumentException("owning team id must be positive");
		}
		Objects.requireNonNull(name, "owning team name must not be null");
		name = name.strip();
		if (name.isEmpty()) {
			throw new IllegalArgumentException("owning team name must not be blank");
		}
	}
}
