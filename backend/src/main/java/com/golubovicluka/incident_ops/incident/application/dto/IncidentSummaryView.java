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
		Instant updatedAt,
		IncidentSlaView sla) {

	public IncidentSummaryView(
			Long id,
			String referenceCode,
			String title,
			IncidentPriority priority,
			IncidentStatus status,
			ManagedServiceView managedService,
			UserView assignee,
			Instant createdAt,
			Instant updatedAt) {
		this(
				id,
				referenceCode,
				title,
				priority,
				status,
				managedService,
				assignee,
				createdAt,
				updatedAt,
				IncidentSlaView.notConfigured());
	}

	public static IncidentSummaryView from(Incident incident) {
		return from(incident, IncidentSlaView.notConfigured());
	}

	public static IncidentSummaryView from(
			Incident incident,
			IncidentSlaView sla) {
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
				incident.updatedAt(),
				sla);
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
