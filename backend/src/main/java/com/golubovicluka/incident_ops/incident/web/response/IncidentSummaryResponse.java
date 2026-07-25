package com.golubovicluka.incident_ops.incident.web.response;

import java.time.Instant;

import com.golubovicluka.incident_ops.incident.application.dto.IncidentSlaView;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentSummaryView;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;

public record IncidentSummaryResponse(
		Long id,
		String referenceCode,
		String title,
		IncidentPriority priority,
		IncidentStatus status,
		ManagedServiceResponse managedService,
		UserResponse assignee,
		Instant createdAt,
		Instant updatedAt,
		SlaResponse sla) {

	public static IncidentSummaryResponse from(IncidentSummaryView incident) {
		return new IncidentSummaryResponse(
				incident.id(),
				incident.referenceCode(),
				incident.title(),
				incident.priority(),
				incident.status(),
				new ManagedServiceResponse(
						incident.managedService().id(),
						incident.managedService().name()),
				UserResponse.from(incident.assignee()),
				incident.createdAt(),
				incident.updatedAt(),
				SlaResponse.from(incident.sla()));
	}

	public record ManagedServiceResponse(Long id, String name) {
	}

	public record UserResponse(Long id, String username, String displayName) {

		static UserResponse from(IncidentSummaryView.UserView user) {
			return user == null
					? null
					: new UserResponse(
							user.id(),
							user.username(),
							user.displayName());
		}
	}

	public record SlaResponse(
			com.golubovicluka.incident_ops.analytics.domain.SlaState state,
			com.golubovicluka.incident_ops.analytics.domain.SlaPhase phase,
			Instant deadline) {

		static SlaResponse from(IncidentSlaView sla) {
			return new SlaResponse(sla.state(), sla.phase(), sla.deadline());
		}
	}
}
