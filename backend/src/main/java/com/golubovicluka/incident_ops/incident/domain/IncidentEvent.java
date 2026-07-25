package com.golubovicluka.incident_ops.incident.domain;

import java.time.Instant;
import java.util.Objects;

public record IncidentEvent(
		Long id,
		IncidentEventKind kind,
		IncidentUser actor,
		IncidentStatus previousStatus,
		IncidentStatus newStatus,
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
		if (kind == IncidentEventKind.STATUS_CHANGED) {
			previousStatus = Objects.requireNonNull(
					previousStatus,
					"previousStatus must not be null for a status change");
			newStatus = Objects.requireNonNull(
					newStatus,
					"newStatus must not be null for a status change");
			if (previousStatus == newStatus) {
				throw new IllegalArgumentException(
						"status change must change the incident status");
			}
		} else if (previousStatus != null || newStatus != null) {
			throw new IllegalArgumentException(
					"only status-change events can contain statuses");
		}
	}

	public static IncidentEvent created(IncidentUser actor, Instant occurredAt) {
		return new IncidentEvent(
				null,
				IncidentEventKind.CREATED,
				actor,
				null,
				null,
				occurredAt);
	}

	public static IncidentEvent statusChanged(
			IncidentUser actor,
			IncidentStatus previousStatus,
			IncidentStatus newStatus,
			Instant occurredAt) {
		return new IncidentEvent(
				null,
				IncidentEventKind.STATUS_CHANGED,
				actor,
				previousStatus,
				newStatus,
				occurredAt);
	}
}
