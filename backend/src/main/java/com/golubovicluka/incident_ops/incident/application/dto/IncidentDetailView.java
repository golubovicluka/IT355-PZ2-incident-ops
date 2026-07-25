package com.golubovicluka.incident_ops.incident.application.dto;

import java.time.Instant;
import java.util.List;

import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentEvent;
import com.golubovicluka.incident_ops.incident.domain.IncidentEventKind;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;

public record IncidentDetailView(
		Long id,
		String referenceCode,
		String title,
		String description,
		IncidentPriority priority,
		IncidentStatus status,
		ManagedServiceView managedService,
		UserView reporter,
		UserView assignee,
		Instant createdAt,
		Instant updatedAt,
		Instant acknowledgedAt,
		Instant resolvedAt,
		List<IncidentStatus> allowedTransitions,
		List<EventView> timeline,
		List<EscalationView> escalations) {

	public IncidentDetailView(
			Long id,
			String referenceCode,
			String title,
			String description,
			IncidentPriority priority,
			IncidentStatus status,
			ManagedServiceView managedService,
			UserView reporter,
			UserView assignee,
			Instant createdAt,
			Instant updatedAt,
			Instant acknowledgedAt,
			Instant resolvedAt,
			List<IncidentStatus> allowedTransitions,
			List<EventView> timeline) {
		this(
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
				allowedTransitions,
				timeline,
				timeline.stream()
						.filter(event -> event.kind() == IncidentEventKind.ESCALATED)
						.map(EscalationView::from)
						.toList());
	}

	public IncidentDetailView {
		allowedTransitions = List.copyOf(allowedTransitions);
		timeline = List.copyOf(timeline);
		escalations = List.copyOf(escalations);
	}

	public static IncidentDetailView from(Incident incident) {
		return new IncidentDetailView(
				incident.id(),
				incident.referenceCode(),
				incident.title(),
				incident.description(),
				incident.priority(),
				incident.status(),
				new ManagedServiceView(
						incident.managedService().id(),
						incident.managedService().name()),
				UserView.from(incident.reporter()),
				UserView.from(incident.assignee()),
				incident.createdAt(),
				incident.updatedAt(),
				incident.acknowledgedAt(),
				incident.resolvedAt(),
				incident.allowedTransitions(),
				incident.events().stream().map(EventView::from).toList());
	}

	public record ManagedServiceView(Long id, String name) {
	}

	public record UserView(Long id, String username, String displayName) {

		static UserView from(IncidentUser user) {
			return user == null
					? null
					: new UserView(user.id(), user.username(), user.displayName());
		}
	}

	public record EventView(
			Long id,
			IncidentEventKind kind,
			UserView actor,
			IncidentStatus previousStatus,
			IncidentStatus newStatus,
			String note,
			Integer escalationLevel,
			String escalationReason,
			Instant occurredAt) {

		public EventView(
				Long id,
				IncidentEventKind kind,
				UserView actor,
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

		static EventView from(IncidentEvent event) {
			return new EventView(
					event.id(),
					event.kind(),
					UserView.from(event.actor()),
					event.previousStatus(),
					event.newStatus(),
					event.note(),
					event.escalationLevel(),
					event.escalationReason(),
					event.occurredAt());
		}
	}

	public record EscalationView(
			int level,
			String reason,
			UserView actor,
			Instant escalatedAt) {

		static EscalationView from(EventView event) {
			return new EscalationView(
					event.escalationLevel(),
					event.escalationReason(),
					event.actor(),
					event.occurredAt());
		}
	}
}
