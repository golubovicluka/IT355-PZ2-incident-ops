package com.golubovicluka.incident_ops.incident.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Incident {

	public static final int MAX_REFERENCE_CODE_LENGTH = 32;
	public static final int MAX_TITLE_LENGTH = 200;
	public static final int MAX_DESCRIPTION_LENGTH = 4000;

	private final Long id;
	private final String referenceCode;
	private final String title;
	private final String description;
	private final IncidentPriority priority;
	private final IncidentStatus status;
	private final IncidentManagedService managedService;
	private final IncidentUser reporter;
	private final IncidentUser assignee;
	private final Instant createdAt;
	private final Instant updatedAt;
	private final Instant acknowledgedAt;
	private final Instant resolvedAt;
	private final List<IncidentEvent> events;

	public Incident(
			Long id,
			String referenceCode,
			String title,
			String description,
			IncidentPriority priority,
			IncidentStatus status,
			IncidentManagedService managedService,
			IncidentUser reporter,
			IncidentUser assignee,
			Instant createdAt,
			Instant updatedAt,
			Instant acknowledgedAt,
			Instant resolvedAt,
			List<IncidentEvent> events) {
		if (id != null && id <= 0) {
			throw new IllegalArgumentException("incident id must be positive");
		}
		this.id = id;
		this.referenceCode = requireText(
				referenceCode,
				"referenceCode",
				MAX_REFERENCE_CODE_LENGTH);
		this.title = requireText(title, "title", MAX_TITLE_LENGTH);
		this.description = requireText(
				description,
				"description",
				MAX_DESCRIPTION_LENGTH);
		this.priority = Objects.requireNonNull(priority, "priority must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.managedService = Objects.requireNonNull(
				managedService,
				"managedService must not be null");
		this.reporter = Objects.requireNonNull(reporter, "reporter must not be null");
		this.assignee = assignee;
		this.createdAt = Objects.requireNonNull(
				createdAt,
				"createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(
				updatedAt,
				"updatedAt must not be null");
		if (updatedAt.isBefore(createdAt)) {
			throw new IllegalArgumentException(
					"updatedAt must not be before createdAt");
		}
		validateLifecycleTimestamps(
				status,
				createdAt,
				updatedAt,
				acknowledgedAt,
				resolvedAt);
		this.acknowledgedAt = acknowledgedAt;
		this.resolvedAt = resolvedAt;
		Objects.requireNonNull(events, "events must not be null");
		if (events.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("events must not contain null");
		}
		this.events = List.copyOf(events);
	}

	public static Incident create(
			String referenceCode,
			String title,
			String description,
			IncidentPriority priority,
			IncidentManagedService managedService,
			IncidentUser reporter,
			IncidentUser assignee,
			Instant createdAt) {
		return new Incident(
				null,
				referenceCode,
				title,
				description,
				priority,
				IncidentStatus.OPEN,
				managedService,
				reporter,
				assignee,
				createdAt,
				createdAt,
				null,
				null,
				List.of(IncidentEvent.created(reporter, createdAt)));
	}

	public Incident update(
			String title,
			String description,
			IncidentPriority priority,
			IncidentManagedService managedService,
			IncidentUser assignee,
			Instant updatedAt) {
		return new Incident(
				id,
				referenceCode,
				title,
				description,
				priority,
				status,
				managedService,
				reporter,
				assignee,
				createdAt,
				updatedAt,
				acknowledgedAt,
				resolvedAt,
				events);
	}

	public Incident transitionTo(
			IncidentStatus nextStatus,
			IncidentUser actor,
			Instant transitionedAt) {
		Objects.requireNonNull(nextStatus, "nextStatus must not be null");
		Objects.requireNonNull(actor, "actor must not be null");
		Objects.requireNonNull(
				transitionedAt,
				"transitionedAt must not be null");
		if (!status.allowedTransitions().contains(nextStatus)) {
			throw new InvalidIncidentStatusTransitionException(
					status,
					nextStatus);
		}
		if (transitionedAt.isBefore(updatedAt)) {
			throw new IllegalArgumentException(
					"transitionedAt must not be before updatedAt");
		}

		Instant nextAcknowledgedAt =
				status == IncidentStatus.OPEN ? transitionedAt : acknowledgedAt;
		Instant nextResolvedAt = switch (nextStatus) {
			case RESOLVED -> transitionedAt;
			case INVESTIGATING -> null;
			default -> resolvedAt;
		};
		List<IncidentEvent> transitionedEvents = new ArrayList<>(events);
		transitionedEvents.add(IncidentEvent.statusChanged(
				actor,
				status,
				nextStatus,
				transitionedAt));
		return new Incident(
				id,
				referenceCode,
				title,
				description,
				priority,
				nextStatus,
				managedService,
				reporter,
				assignee,
				createdAt,
				transitionedAt,
				nextAcknowledgedAt,
				nextResolvedAt,
				transitionedEvents);
	}

	public Incident addNote(
			String note,
			IncidentUser actor,
			Instant notedAt) {
		Objects.requireNonNull(actor, "actor must not be null");
		Objects.requireNonNull(notedAt, "notedAt must not be null");
		if (notedAt.isBefore(updatedAt)) {
			throw new IllegalArgumentException(
					"notedAt must not be before updatedAt");
		}
		List<IncidentEvent> notedEvents = new ArrayList<>(events);
		notedEvents.add(IncidentEvent.noteAdded(actor, note, notedAt));
		return new Incident(
				id,
				referenceCode,
				title,
				description,
				priority,
				status,
				managedService,
				reporter,
				assignee,
				createdAt,
				notedAt,
				acknowledgedAt,
				resolvedAt,
				notedEvents);
	}

	public Long id() {
		return id;
	}

	public String referenceCode() {
		return referenceCode;
	}

	public String title() {
		return title;
	}

	public String description() {
		return description;
	}

	public IncidentPriority priority() {
		return priority;
	}

	public IncidentStatus status() {
		return status;
	}

	public IncidentManagedService managedService() {
		return managedService;
	}

	public IncidentUser reporter() {
		return reporter;
	}

	public IncidentUser assignee() {
		return assignee;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public Instant acknowledgedAt() {
		return acknowledgedAt;
	}

	public Instant resolvedAt() {
		return resolvedAt;
	}

	public List<IncidentStatus> allowedTransitions() {
		return status.allowedTransitions();
	}

	public List<IncidentEvent> events() {
		return events;
	}

	private static void validateLifecycleTimestamps(
			IncidentStatus status,
			Instant createdAt,
			Instant updatedAt,
			Instant acknowledgedAt,
			Instant resolvedAt) {
		if (acknowledgedAt != null
				&& (acknowledgedAt.isBefore(createdAt)
				|| acknowledgedAt.isAfter(updatedAt))) {
			throw new IllegalArgumentException(
					"acknowledgedAt must be within the incident lifetime");
		}
		if (resolvedAt != null
				&& (resolvedAt.isBefore(createdAt)
				|| resolvedAt.isAfter(updatedAt))) {
			throw new IllegalArgumentException(
					"resolvedAt must be within the incident lifetime");
		}
		switch (status) {
			case OPEN -> {
				if (acknowledgedAt != null || resolvedAt != null) {
					throw new IllegalArgumentException(
							"open incident cannot have lifecycle timestamps");
				}
			}
			case ACKNOWLEDGED, INVESTIGATING -> {
				if (acknowledgedAt == null || resolvedAt != null) {
					throw new IllegalArgumentException(
							"active incident must be acknowledged and unresolved");
				}
			}
			case RESOLVED, CLOSED -> {
				if (acknowledgedAt == null || resolvedAt == null) {
					throw new IllegalArgumentException(
							"resolved incident must have lifecycle timestamps");
				}
			}
		}
	}

	private static String requireText(
			String value,
			String field,
			int maxLength) {
		Objects.requireNonNull(value, field + " must not be null");
		String normalized = value.strip();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(
					field + " must not exceed " + maxLength + " characters");
		}
		return normalized;
	}
}
