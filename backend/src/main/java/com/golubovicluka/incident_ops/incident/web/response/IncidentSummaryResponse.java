package com.golubovicluka.incident_ops.incident.web.response;

import java.time.Instant;

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
		Instant updatedAt) {

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
				incident.updatedAt());
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
}
