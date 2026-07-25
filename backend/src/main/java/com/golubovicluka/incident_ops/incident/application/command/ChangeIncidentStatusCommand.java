package com.golubovicluka.incident_ops.incident.application.command;

import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;

public record ChangeIncidentStatusCommand(
		long id,
		IncidentStatus status,
		String actorUsername) {
}
