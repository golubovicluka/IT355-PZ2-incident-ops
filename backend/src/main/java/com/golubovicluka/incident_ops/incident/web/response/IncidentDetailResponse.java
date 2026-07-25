package com.golubovicluka.incident_ops.incident.web.response;

import java.time.Instant;
import java.util.List;

import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.domain.IncidentEventKind;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;

public record IncidentDetailResponse(
		Long id,
		String referenceCode,
		String title,
		String description,
		IncidentPriority priority,
		IncidentStatus status,
		ManagedServiceResponse managedService,
		UserResponse reporter,
		UserResponse assignee,
		Instant createdAt,
		Instant updatedAt,
		Instant acknowledgedAt,
		Instant resolvedAt,
		List<IncidentStatus> allowedTransitions,
		List<EventResponse> timeline) {

	public IncidentDetailResponse {
		allowedTransitions = List.copyOf(allowedTransitions);
		timeline = List.copyOf(timeline);
	}

	public static IncidentDetailResponse from(IncidentDetailView incident) {
		return new IncidentDetailResponse(
				incident.id(),
				incident.referenceCode(),
				incident.title(),
				incident.description(),
				incident.priority(),
				incident.status(),
				new ManagedServiceResponse(
						incident.managedService().id(),
						incident.managedService().name()),
				UserResponse.from(incident.reporter()),
				UserResponse.from(incident.assignee()),
				incident.createdAt(),
				incident.updatedAt(),
				incident.acknowledgedAt(),
				incident.resolvedAt(),
				incident.allowedTransitions(),
				incident.timeline().stream().map(EventResponse::from).toList());
	}

	public record ManagedServiceResponse(Long id, String name) {
	}

	public record UserResponse(Long id, String username, String displayName) {

		static UserResponse from(IncidentDetailView.UserView user) {
			return user == null
					? null
					: new UserResponse(
							user.id(),
							user.username(),
							user.displayName());
		}
	}

	public record EventResponse(
			Long id,
			IncidentEventKind kind,
			UserResponse actor,
			IncidentStatus previousStatus,
			IncidentStatus newStatus,
			Instant occurredAt) {

		static EventResponse from(IncidentDetailView.EventView event) {
			return new EventResponse(
					event.id(),
					event.kind(),
					UserResponse.from(event.actor()),
					event.previousStatus(),
					event.newStatus(),
					event.occurredAt());
		}
	}
}
