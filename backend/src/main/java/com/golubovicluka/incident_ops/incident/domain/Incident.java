package com.golubovicluka.incident_ops.incident.domain;

import java.time.Instant;
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
				events);
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

	public List<IncidentEvent> events() {
		return events;
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
