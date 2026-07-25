package com.golubovicluka.incident_ops.incident.domain;

import java.time.Instant;
import java.util.Objects;

public record IncidentEvent(
		Long id,
		IncidentEventKind kind,
		IncidentUser actor,
		IncidentStatus previousStatus,
		IncidentStatus newStatus,
		String note,
		Integer escalationLevel,
		String escalationReason,
		Instant occurredAt) {

	public static final int MAX_NOTE_LENGTH = 2000;
	public static final int MAX_ESCALATION_REASON_LENGTH = 1000;

	public IncidentEvent(
			Long id,
			IncidentEventKind kind,
			IncidentUser actor,
			IncidentStatus previousStatus,
			IncidentStatus newStatus,
			String note,
			Instant occurredAt) {
		this(
				id,
				kind,
				actor,
				previousStatus,
				newStatus,
				note,
				null,
				null,
				occurredAt);
	}

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
		if (kind == IncidentEventKind.NOTE_ADDED) {
			note = requireNote(note);
		} else if (note != null) {
			throw new IllegalArgumentException(
					"only note events can contain note text");
		}
		if (kind == IncidentEventKind.ESCALATED) {
			if (escalationLevel == null || escalationLevel <= 0) {
				throw new IllegalArgumentException(
						"escalation level must be positive");
			}
			escalationReason = requireEscalationReason(escalationReason);
		} else if (escalationLevel != null || escalationReason != null) {
			throw new IllegalArgumentException(
					"only escalation events can contain escalation details");
		}
	}

	public static IncidentEvent created(IncidentUser actor, Instant occurredAt) {
		return new IncidentEvent(
				null,
				IncidentEventKind.CREATED,
				actor,
				null,
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
				null,
				occurredAt);
	}

	public static IncidentEvent noteAdded(
			IncidentUser actor,
			String note,
			Instant occurredAt) {
		return new IncidentEvent(
				null,
				IncidentEventKind.NOTE_ADDED,
				actor,
				null,
				null,
				note,
				occurredAt);
	}

	public static IncidentEvent escalated(
			IncidentUser actor,
			int level,
			String reason,
			Instant occurredAt) {
		return new IncidentEvent(
				null,
				IncidentEventKind.ESCALATED,
				actor,
				null,
				null,
				null,
				level,
				reason,
				occurredAt);
	}

	private static String requireNote(String value) {
		Objects.requireNonNull(value, "note must not be null");
		String normalized = value.strip();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("note must not be blank");
		}
		if (normalized.length() > MAX_NOTE_LENGTH) {
			throw new IllegalArgumentException(
					"note must not exceed " + MAX_NOTE_LENGTH + " characters");
		}
		return normalized;
	}

	private static String requireEscalationReason(String value) {
		Objects.requireNonNull(value, "escalation reason must not be null");
		String normalized = value.strip();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(
					"escalation reason must not be blank");
		}
		if (normalized.length() > MAX_ESCALATION_REASON_LENGTH) {
			throw new IllegalArgumentException(
					"escalation reason must not exceed "
							+ MAX_ESCALATION_REASON_LENGTH
							+ " characters");
		}
		return normalized;
	}
}
