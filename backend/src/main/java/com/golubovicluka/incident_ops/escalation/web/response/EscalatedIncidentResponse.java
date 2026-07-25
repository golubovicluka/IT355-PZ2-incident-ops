package com.golubovicluka.incident_ops.escalation.web.response;

import java.time.Instant;
import java.util.List;

import com.golubovicluka.incident_ops.analytics.domain.SlaPhase;
import com.golubovicluka.incident_ops.analytics.domain.SlaState;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentSlaView;
import com.golubovicluka.incident_ops.incident.domain.IncidentEventKind;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;

public record EscalatedIncidentResponse(
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
		List<EventResponse> timeline,
		List<EscalationResponse> escalations,
		SlaResponse sla) {

	public EscalatedIncidentResponse {
		allowedTransitions = List.copyOf(allowedTransitions);
		timeline = List.copyOf(timeline);
		escalations = List.copyOf(escalations);
	}

	public static EscalatedIncidentResponse from(IncidentDetailView incident) {
		return new EscalatedIncidentResponse(
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
				incident.timeline().stream().map(EventResponse::from).toList(),
				incident.escalations().stream()
						.map(EscalationResponse::from)
						.toList(),
				SlaResponse.from(incident.sla()));
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
			String note,
			Integer escalationLevel,
			String escalationReason,
			Instant occurredAt) {

		static EventResponse from(IncidentDetailView.EventView event) {
			return new EventResponse(
					event.id(),
					event.kind(),
					UserResponse.from(event.actor()),
					event.previousStatus(),
					event.newStatus(),
					event.note(),
					event.escalationLevel(),
					event.escalationReason(),
					event.occurredAt());
		}
	}

	public record EscalationResponse(
			int level,
			String reason,
			UserResponse actor,
			Instant escalatedAt) {

		static EscalationResponse from(
				IncidentDetailView.EscalationView escalation) {
			return new EscalationResponse(
					escalation.level(),
					escalation.reason(),
					UserResponse.from(escalation.actor()),
					escalation.escalatedAt());
		}
	}

	public record SlaResponse(
			SlaState state,
			SlaPhase phase,
			Instant deadline) {

		static SlaResponse from(IncidentSlaView sla) {
			return new SlaResponse(sla.state(), sla.phase(), sla.deadline());
		}
	}
}
