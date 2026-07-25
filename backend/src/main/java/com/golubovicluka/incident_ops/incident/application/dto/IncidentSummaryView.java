package com.golubovicluka.incident_ops.incident.application.dto;

import java.time.Instant;

import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
import com.golubovicluka.incident_ops.incident.domain.IncidentUser;

public record IncidentSummaryView(
		Long id,
		String referenceCode,
		String title,
		IncidentPriority priority,
		IncidentStatus status,
		ManagedServiceView managedService,
		UserView assignee,
		Instant createdAt,
		Instant updatedAt) {

	public static IncidentSummaryView from(Incident incident) {
		return new IncidentSummaryView(
				incident.id(),
				incident.referenceCode(),
				incident.title(),
				incident.priority(),
				incident.status(),
				new ManagedServiceView(
						incident.managedService().id(),
						incident.managedService().name()),
				UserView.from(incident.assignee()),
				incident.createdAt(),
				incident.updatedAt());
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
}
