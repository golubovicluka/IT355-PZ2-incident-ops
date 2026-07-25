package com.golubovicluka.incident_ops.escalation.domain;

import java.time.Instant;
import java.util.Objects;

import com.golubovicluka.incident_ops.incident.domain.IncidentEvent;

public record Escalation(
		Long id,
		long incidentId,
		int level,
		String reason,
		EscalationActor actor,
		Instant escalatedAt) {

	public Escalation {
		if (id != null && id <= 0) {
			throw new IllegalArgumentException("escalation id must be positive");
		}
		if (incidentId <= 0) {
			throw new IllegalArgumentException("incident id must be positive");
		}
		if (level <= 0) {
			throw new IllegalArgumentException("escalation level must be positive");
		}
		reason = requireReason(reason);
		actor = Objects.requireNonNull(actor, "actor must not be null");
		escalatedAt = Objects.requireNonNull(
				escalatedAt,
				"escalatedAt must not be null");
	}

	public static Escalation create(
			long incidentId,
			int level,
			String reason,
			EscalationActor actor,
			Instant escalatedAt) {
		return new Escalation(
				null,
				incidentId,
				level,
				reason,
				actor,
				escalatedAt);
	}

	public static int nextLevel(int highestLevel) {
		if (highestLevel < 0 || highestLevel == Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
					"highest escalation level cannot be incremented");
		}
		return highestLevel + 1;
	}

	private static String requireReason(String value) {
		Objects.requireNonNull(value, "reason must not be null");
		String normalized = value.strip();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("reason must not be blank");
		}
		if (normalized.length() > IncidentEvent.MAX_ESCALATION_REASON_LENGTH) {
			throw new IllegalArgumentException(
					"reason must not exceed "
							+ IncidentEvent.MAX_ESCALATION_REASON_LENGTH
							+ " characters");
		}
		return normalized;
	}
}
