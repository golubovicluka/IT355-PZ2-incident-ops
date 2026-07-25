package com.golubovicluka.incident_ops.incident.domain;

import java.time.Instant;
import java.util.Objects;

public record IncidentEvent(
		Long id,
		IncidentEventKind kind,
		IncidentUser actor,
		Instant occurredAt) {

	public IncidentEvent {
		if (id != null && id <= 0) {
			throw new IllegalArgumentException("event id must be positive");
		}
		kind = Objects.requireNonNull(kind, "kind must not be null");
		actor = Objects.requireNonNull(actor, "actor must not be null");
		occurredAt = Objects.requireNonNull(
				occurredAt,
				"occurredAt must not be null");
	}

	public static IncidentEvent created(IncidentUser actor, Instant occurredAt) {
		return new IncidentEvent(
				null,
				IncidentEventKind.CREATED,
				actor,
				occurredAt);
	}
}
