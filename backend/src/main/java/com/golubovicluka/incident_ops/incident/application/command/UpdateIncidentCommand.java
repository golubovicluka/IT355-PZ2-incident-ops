package com.golubovicluka.incident_ops.incident.application.command;

import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;

public record UpdateIncidentCommand(
		long id,
		String title,
		String description,
		IncidentPriority priority,
		long managedServiceId,
		Long assigneeId) {
}
